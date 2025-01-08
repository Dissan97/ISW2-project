package it.torvergata.dissanuddinahmed.controller;

import it.torvergata.dissanuddinahmed.model.Release;
import it.torvergata.dissanuddinahmed.model.Ticket;
import it.torvergata.dissanuddinahmed.utilities.Sink;
import it.torvergata.dissanuddinahmed.utilities.WebJsonReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;


import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;


public class JiraInjection {

    private static final String  RELEASE_DATE = "releaseDate";
    private final String projName;
    private List<Release> releases = null;
    private List<Release> affectedReleases = null;
    private List<Ticket> ticketsWithIssues = null;
    private List<Ticket> fixedTickets = null;

    public JiraInjection(String projectName) {
        this.projName = projectName;
    }

    public void injectReleases() throws IOException, URISyntaxException {
        this.releases = new ArrayList<>();
        int i = 0;
        String url = "https://issues.apache.org/jira/rest/api/latest/project/" + this.projName;
        JSONObject object = WebJsonReader.readJsonFromUrl(url);
        JSONArray versions = object.getJSONArray("versions");
        for (; i < versions.length(); i++) {
            String releaseName;
            String releaseDate;
            JSONObject releaseJsonObject = versions.getJSONObject(i);
            if (releaseJsonObject.has(RELEASE_DATE) && releaseJsonObject.has("name")) {
                releaseDate = releaseJsonObject.get(RELEASE_DATE).toString();
                releaseName = releaseJsonObject.get("name").toString();
                releases.add(new Release(releaseName, LocalDate.parse(releaseDate)));
            }
        }
        releases.sort(Comparator.comparing(Release::releaseDate));
        i = 0;
        for (Release release : releases) {
            release.setId(++i);
        }
    }

    public void injectTickets() throws IOException, URISyntaxException {
        this.pullIssues();
        this.filterFixedApplyingProportion();

    }

    public void filterFixedNormally() {
        this.fixedTickets = new ArrayList<>();
        for (Ticket ticket : this.ticketsWithIssues) {
            if (ticket.isCorrectTicket()) {
                this.fixedTickets.add(ticket);
            }
        }
        this.fixedTickets.sort(Comparator.comparing(Ticket::getResolutionDate));
    }

    /**
     * This method is used to load tickets from JIRA
     */

    public void pullIssues() throws IOException, URISyntaxException {
        int j;
        int i = 0;
        int total;
        this.ticketsWithIssues = new ArrayList<>();
        do {
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + this.projName + "%22AND%22issueType%22=%22Bug%22AND" +
                    "(%22status%22=%22Closed%22OR%22status%22=%22Resolved%22)" +
                    "AND%22resolution%22=%22Fixed%22&fields=key,versions,created,resolutiondate&startAt="
                    + i + "&maxResults=" + j;
            JSONObject json = WebJsonReader.readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            for (; i < total && i < j; i++) {
                //Iterate through each bug
                String key = issues.getJSONObject(i%1000).get("key").toString();
                JSONObject fields = issues.getJSONObject(i%1000).getJSONObject("fields");
                String creationDateString = fields.get("created").toString();
                String resolutionDateString = fields.get("resolutiondate").toString();
                LocalDate creationDate = LocalDate.parse(creationDateString.substring(0,10));
                LocalDate resolutionDate = LocalDate.parse(resolutionDateString.substring(0,10));
                JSONArray affectedVersionsArray = fields.getJSONArray("versions");
                Release openingVersion = getReleaseAfterOrEqualDate(creationDate, this.releases);
                Release fixedVersion =  getReleaseAfterOrEqualDate(resolutionDate, this.releases);
                checkValidAffectedVersions(affectedVersionsArray);
                if(!this.affectedReleases.isEmpty()
                        && openingVersion!=null
                        && fixedVersion!=null
                        && (!this.affectedReleases.get(0).releaseDate().isBefore(openingVersion.releaseDate())
                        || openingVersion.releaseDate().isAfter(fixedVersion.releaseDate()))){
                    continue;
                }
                if(openingVersion != null && fixedVersion != null && openingVersion.id() !=
                        this.releases.get(0).id()){
                    this.ticketsWithIssues.add(new Ticket(key, creationDate, resolutionDate, openingVersion,
                            fixedVersion, this.affectedReleases));
                }
            }
        } while (i < total);
        this.ticketsWithIssues.sort(Comparator.comparing(Ticket::getResolutionDate));

    }

    private void filterFixedApplyingProportion() {
        List<Ticket> proportionTickets = new ArrayList<>();
        this.fixedTickets = new ArrayList<>();
        double proportion;
        PreProcessProportion preProcessProportion = new PreProcessProportion();
        JSONObject proportionResults = new JSONObject();
        for(Ticket ticket : this.getTicketsWithIssues()){
            if(!ticket.isCorrectTicket()){
                proportion = preProcessProportion.computeProportion(proportionTickets,
                        ticket, true, proportionResults);
                this.preProcessTicketWithProportion(ticket, proportion);
                this.adjustAffectedVersions(ticket);
            }else{
                preProcessProportion.computeProportion(proportionTickets, ticket,
                        false, proportionResults);
                this.adjustAffectedVersions(ticket);
                proportionTickets.add(ticket);
            }
            this.fixedTickets.add(ticket);
        }
        this.fixedTickets.sort(Comparator.comparing(Ticket::getResolutionDate));

        Sink.serializeToJson(this.projName, "Proportion", proportionResults,
                Sink.FileExtension.JSON);



    }


    private void preProcessTicketWithProportion(@NotNull Ticket ticket, double proportion) {
        List<Release> affectedVersionsList = new ArrayList<>();
        int injectedVersionId;
        //IV = max(1; FV-(FV-OV)*P)
        if(ticket.getFixedVersion().id() == ticket.getOpeningVersion().id()){
            injectedVersionId = Math.max(1, (int) (ticket.getFixedVersion().id()-proportion));
        }else{
            injectedVersionId = Math.max(1, (int) (ticket.getFixedVersion().id()
                    -((ticket.getFixedVersion().id()-ticket.getOpeningVersion().id())*proportion)));
        }
        for (Release release : this.releases){
            if(release.id() == injectedVersionId){
                affectedVersionsList.add(new Release(release.id(), release.releaseName(), release.releaseDate()));
                break;
            }
        }
        affectedVersionsList.sort(Comparator.comparing(Release::releaseDate));
        ticket.setAffectedVersions(affectedVersionsList);
        ticket.setInjectedVersion(affectedVersionsList.get(0));
    }
    private void adjustAffectedVersions(@NotNull Ticket ticket) {
        List<Release> completeAffectedVersionsList = new ArrayList<>();
        for(int i = ticket.getInjectedVersion().id(); i < ticket.getFixedVersion().id(); i++){
            for(Release release : this.releases){
                if(release.id() == i){
                    completeAffectedVersionsList.add(new Release(release.id(),
                            release.releaseName(), release.releaseDate()));
                    break;
                }
            }
        }
        completeAffectedVersionsList.sort(Comparator.comparing(Release::releaseDate));
        ticket.setAffectedVersions(completeAffectedVersionsList);
    }

    private void checkValidAffectedVersions(@NotNull JSONArray affectedVersionsArray) {
        this.affectedReleases = new ArrayList<>();
        for (int i = 0; i < affectedVersionsArray.length(); i++) {
            String affectedVersionName = affectedVersionsArray.getJSONObject(i).get("name").toString();
            for (Release release : this.releases) {
                if (Objects.equals(affectedVersionName, release.releaseName())) {
                    this.affectedReleases.add(release);
                    break;
                }
            }
        }
        this.affectedReleases.sort(Comparator.comparing(Release::releaseDate));

    }

    private @Nullable Release getReleaseAfterOrEqualDate(LocalDate specificDate, @NotNull List<Release> releasesList) {
        releasesList.sort(Comparator.comparing(Release::releaseDate));
        for (Release release : releasesList) {
            if (!release.releaseDate().isBefore(specificDate)) {
                return release;
            }
        }
        return null;
    }

    public List<Release> getReleases() {
        return this.releases;
    }

    public List<Ticket> getTicketsWithIssues() {
        return this.ticketsWithIssues;
    }

    public List<Ticket> getTicketsWithAffectedVersion (){
        List<Ticket> affectedVersions = new ArrayList<>();
        for (Ticket ticket : this.ticketsWithIssues) {
            if (ticket.isCorrectTicket()) {
                affectedVersions.add(ticket);
            }
        }
        affectedVersions.sort(Comparator.comparing(Ticket::getResolutionDate));
        return affectedVersions;
    }

    public List<Ticket> getFixedTickets() {
        return this.fixedTickets;
    }

    public Map<String, String> getMapReleases() {
        Map<String, String> retMap = new HashMap<>();
        this.releases.sort(Comparator.comparing(Release::releaseDate));
        final String name = "name";
        final String commits = "commits";
        for (Release release : this.releases) {
            Map<String, String> inner = new LinkedHashMap<>();
            inner.put(name, release.releaseName());
            inner.put(RELEASE_DATE, release.releaseDate().toString());
            inner.put(commits, String.valueOf(release.getCommitList().size()));
            retMap.put(String.valueOf(release.id()), inner.toString());
        }
        return retMap;
    }
}
