package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    Injector injector = Guice.createInjector(
            new WebCrawlerModule(config),
            new ProfilerModule()
    );
    injector.injectMembers(this);

    CrawlResult crawlResult = crawler.crawl(config.getStartPages());
    CrawlResultWriter crawlResultWriter = new CrawlResultWriter(crawlResult);
    Path resultPath = Paths.get(config.getResultPath());
    Writer resultFileWriter = config.getResultPath().isEmpty() ?
            new OutputStreamWriter(System.out) :
            Files.newBufferedWriter(resultPath);
    crawlResultWriter.write(resultFileWriter);

    Path profilePath = Paths.get(config.getProfileOutputPath());
    Writer profileFileWriter = config.getProfileOutputPath().isEmpty() ?
            new OutputStreamWriter(System.out) :
            Files.newBufferedWriter(profilePath);
    profiler.writeData(profileFileWriter);

    if (config.getResultPath().isEmpty() || config.getProfileOutputPath().isEmpty()) {
        resultFileWriter.flush();
        profileFileWriter.flush();
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();
  }
}
