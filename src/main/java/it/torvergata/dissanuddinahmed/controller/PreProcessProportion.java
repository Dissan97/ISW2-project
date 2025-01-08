package it.torvergata.dissanuddinahmed.controller;

import it.torvergata.dissanuddinahmed.model.Ticket;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Logger;

public class PreProcessProportion {

    public static final String NAME_OF_THIS_CLASS = PreProcessProportion.class.getName();
    private static final Logger logger = Logger.getLogger(NAME_OF_THIS_CLASS);
    private static final String TICKET_SIZE = "ticket_size";
    private static final String AVERAGE_PROPORTION = "average_proportion";
    private static final String MESSAGE_PROPORTION = "no changes";
    private static final String DENOMINATOR = "denominator=1";
    public static final int THRESHOLD_FOR_COLD_START = 5;
    private static final String COLD_START = "COLD_START_PROPORTIONS";
    private static final String COLD_START_MEDIAN = "COLD_START_MEDIAN";
    private static final String PROJECT_ANALYZED = "PROJECTS";
    private static final String COLD_START_ANALYZE = "(COLD_START_ANALYZE)";


    private enum OtherProjects {
        AVRO,
        SYNCOPE,
        STORM,
        TAJO,
        ZOOKEEPER
    }

    private static Double coldStartComputedProportion = null;

    private static double incrementalProportionComputation(List<Ticket> filteredTicketsList,
                                                           Ticket ticket, boolean newEntry, boolean computation,
                                                           JSONObject reportJson) {
        if (writeUsedOrNot(ticket, computation, newEntry, reportJson)) return 0;
        filteredTicketsList.sort(Comparator.comparing(Ticket::getResolutionDate));
        double totalProportion = getTotalProportion(filteredTicketsList);
        double mean = totalProportion / filteredTicketsList.size();
        if (newEntry) {
            JSONObject sizeAndMean = new JSONObject();
            sizeAndMean.put(TICKET_SIZE, filteredTicketsList.size());
            sizeAndMean.put(AVERAGE_PROPORTION, mean);
            reportJson.put(ticket.getTicketKey(), sizeAndMean);
        }

        return mean;
    }


    private static double getTotalProportion(List<Ticket> tickets) {
        double totalProportion = 0.0;
        double denominator;
        for (Ticket correctTicket : tickets) {
            if (correctTicket.getFixedVersion().id() != correctTicket.getOpeningVersion().id()) {
                denominator = ((double) correctTicket.getFixedVersion().id() -
                        (double) correctTicket.getOpeningVersion().id());
            } else {
                denominator = 1;
            }
            double propForTicket = ((double) correctTicket.getFixedVersion().id() -
                    (double) correctTicket.getInjectedVersion().id()) / denominator;
            totalProportion += propForTicket;
        }
        return totalProportion;
    }

    private static boolean writeUsedOrNot(Ticket ticket, boolean compute, boolean newEntry, JSONObject reportJson) {
        if (!compute && newEntry) {

            JSONObject entry = new JSONObject();
            if (ticket.getFixedVersion().id() != ticket.getOpeningVersion().id()) {
                entry.put(AVERAGE_PROPORTION, MESSAGE_PROPORTION);

            } else {
                entry.put(AVERAGE_PROPORTION, DENOMINATOR);
            }

            reportJson.put(ticket.getTicketKey(), entry);
            return true;
        }
        return false;
    }

    private static synchronized void addProportion(OtherProjects projName, JSONArray array,
                                                   @NotNull List<Double> proportions, double proportion) {
        JSONObject entry = new JSONObject();
        entry.put("name", projName.name());
        entry.put("mean_proportion", proportion);
        proportions.add(proportion);
        array.put(entry);
    }

    private static synchronized double coldStartProportionComputation(Ticket ticket, boolean doActualComputation,
                                                  JSONObject reportJson) {
        writeUsedOrNot(ticket, doActualComputation, false, reportJson);
        JSONObject entry = new JSONObject();
        JSONArray array = new JSONArray();
        if (coldStartComputedProportion != null) {
            entry.put(AVERAGE_PROPORTION, coldStartComputedProportion);
            entry.put(COLD_START, true);
            return PreProcessProportion.coldStartComputedProportion;
        }
        List<Double> proportionList = new ArrayList<>();
        logger.info("called cold start");

        for (OtherProjects projName : OtherProjects.values()) {

            JiraInjection jiraInjection = new JiraInjection(projName.toString());
            try {

                jiraInjection.injectReleases();

                jiraInjection.pullIssues();

                jiraInjection.filterFixedNormally();

            } catch (IOException | URISyntaxException e) {
                logger.severe(e.getMessage());
            }

            List<Ticket> filteredTickets = jiraInjection.getTicketsWithAffectedVersion();
            if (filteredTickets.size() >= THRESHOLD_FOR_COLD_START) {
                PreProcessProportion.addProportion(projName, array, proportionList, incrementalProportionComputation(
                        filteredTickets, ticket, false, doActualComputation, reportJson));
            }


        }


        Collections.sort(proportionList);
        double median;

        int size = proportionList.size();

        if (size % 2 == 0) {
            median = (proportionList.get((size / 2) - 1) + proportionList.get(size / 2)) / 2;
        } else {
            median = proportionList.get(size / 2);
        }
        entry.put(COLD_START_MEDIAN, median);
        entry.put(PROJECT_ANALYZED, array);
        reportJson.put(COLD_START_ANALYZE, entry);

        coldStartComputedProportion = median;
        return median;
    }

    public double computeProportion(List<Ticket> fixedTicketsList,
                                    Ticket ticket, boolean doActualComputation,
                                    JSONObject reportJson) {
        double proportion;
        if (fixedTicketsList.size() >= THRESHOLD_FOR_COLD_START) {
            proportion = PreProcessProportion.incrementalProportionComputation(fixedTicketsList, ticket,
                    true, doActualComputation, reportJson);
        } else {
            proportion = coldStartProportionComputation(ticket, doActualComputation, reportJson);
        }
        return proportion;
    }

}