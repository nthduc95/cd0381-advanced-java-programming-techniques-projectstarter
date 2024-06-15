package com.udacity.webcrawler;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.time.Clock;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;


/**
 * A class that crawls data from a given URL and its links.
 */
public class DataCrawler extends RecursiveTask<Boolean> {
    private final String url;
    private final int maxDepth;
    private final Instant deadline;
    private final ConcurrentMap<String, Integer> counts;
    private final ConcurrentSkipListSet<String> visitedUrls;
    private final List<Pattern> ignoredUrls;
    private final Clock clock;
    private final PageParserFactory parserFactory;

    public DataCrawler(String url, int maxDepth, Instant deadline, ConcurrentMap<String, Integer> counts,
                       ConcurrentSkipListSet<String> visitedUrls, List<Pattern> ignoredUrls, Clock clock,
                       PageParserFactory parserFactory) {
        this.url = url;
        this.maxDepth = maxDepth;
        this.deadline = deadline;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
        this.clock = clock;
        this.parserFactory = parserFactory;
    }

    /**
     * Computes the crawling task.
     *
     * @return true if the task was successful, false otherwise.
     */
    @Override
    protected Boolean compute() {
        if (shouldStopCrawling()) {
            return (Boolean) false;
        }

        visitedUrls.add(url);
        PageParser.Result result = parsePage();

        updateWordCounts(result);
        crawlLinks(result);

        return (Boolean) true;
    }

    private boolean shouldStopCrawling() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return true;
        }

        if (isUrlIgnored() || visitedUrls.contains(url)) {
            return true;
        }

        return false;
    }

    private boolean isUrlIgnored() {
        return ignoredUrls.stream().anyMatch(pattern -> pattern.matcher(url).matches());
    }

    private PageParser.Result parsePage() {
        return parserFactory.get(url).parse();
    }

    private void updateWordCounts(PageParser.Result result) {
        for (ConcurrentMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.compute(e.getKey(), (k, v) -> (v == null) ? e.getValue() : (Integer) (e.getValue() + v));
        }
    }

    private void crawlLinks(PageParser.Result result) {
        List<DataCrawler> subtasks = new ArrayList<>();
        for (String link : result.getLinks()) {
            subtasks.add(new DataCrawler(link, maxDepth - 1, deadline, counts, visitedUrls, ignoredUrls, clock,
                    parserFactory));
        }
        invokeAll(subtasks);
    }
}