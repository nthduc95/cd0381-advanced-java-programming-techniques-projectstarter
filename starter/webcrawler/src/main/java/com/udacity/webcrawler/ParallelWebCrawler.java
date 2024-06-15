package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;
  private final PageParserFactory parserFactory;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          @Timeout Duration timeout,
          @PopularWordCount int popularWordCount,
          @TargetParallelism int threadCount,
          @MaxDepth int maxDepth,
          @IgnoredUrls List<Pattern> ignoredUrls,
          PageParserFactory parserFactory) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
    this.parserFactory = parserFactory;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    ConcurrentMap<String, Integer> wordCounts = new ConcurrentHashMap<>();
    ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();

    startingUrls.forEach(url -> pool.invoke(createCrawler(url, maxDepth, deadline, wordCounts, visitedUrls, ignoredUrls, clock, parserFactory)));



    return new CrawlResult.Builder()
            .setWordCounts(wordCounts.isEmpty() ? wordCounts : WordCounts.sort(wordCounts, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  private DataCrawler createCrawler(String url, int maxDepth, Instant deadline, ConcurrentMap<String, Integer> wordCounts,
                                    ConcurrentSkipListSet<String> visitedUrls, List<Pattern> ignoredUrls, Clock clock,
                                    PageParserFactory parserFactory) {
    return new DataCrawler(url, maxDepth, deadline, wordCounts, visitedUrls, ignoredUrls, clock, parserFactory);
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
