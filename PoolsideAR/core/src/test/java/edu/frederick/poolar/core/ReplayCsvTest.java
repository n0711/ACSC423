package edu.frederick.poolar.core;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.Test;

public class ReplayCsvTest {
  private static final String VALID = "0,red1,RED,1,0,2,1\n1000,red1,RED,3,0,2,1\n";

  private Replay parse(String rows) throws IOException {
    return ReplayCsv.read(new StringReader(ReplayCsv.HEADER + "\n" + rows), Sport.WATER_POLO);
  }

  @Test
  public void readsValidCsv() throws Exception {
    assertEquals(2, parse(VALID).frames.size());
  }

  @Test
  public void supportsCrLf() throws Exception {
    assertEquals(
        2,
        ReplayCsv.read(
                new StringReader((ReplayCsv.HEADER + "\n" + VALID).replace("\n", "\r\n")),
                Sport.WATER_POLO)
            .frames
            .size());
  }

  @Test
  public void rejectsInvalidHeader() {
    assertThrows(
        IOException.class, () -> ReplayCsv.read(new StringReader("bad"), Sport.WATER_POLO));
  }

  @Test
  public void reportsLineNumber() {
    IOException error = assertThrows(IOException.class, () -> parse("0,a,RED,NaN,0,0,1\n"));
    assertTrue(error.getMessage().startsWith("Line 2:"));
  }

  @Test
  public void rejectsInvalidRows() {
    String[] bad = {
      "0,a,RED,1,0,0,1,extra",
      "0,a,RED,31,0,0,1",
      "0,a,RED,1,6,0,1",
      "0,a,RED,1,0,21,1",
      "0,a,RED,1,0,0,1.1",
      "0,a,SWIMMER,1,0,0,1",
      "-1,a,RED,1,0,0,1",
      "1,a,RED,1,0,0,1",
      "0,a,RED,Infinity,0,0,1"
    };
    for (String row : bad) assertThrows(row, IOException.class, () -> parse(row + "\n"));
  }

  @Test
  public void rejectsDuplicateEntity() {
    assertThrows(
        IOException.class, () -> parse("0,a,RED,1,0,0,1\n0,a,RED,1,0,0,1\n1000,a,RED,1,0,0,1"));
  }

  @Test
  public void rejectsMultipleBalls() {
    assertThrows(
        IOException.class, () -> parse("0,a,BALL,1,0,0,1\n0,b,BALL,2,0,0,1\n1000,a,BALL,1,0,0,1"));
  }

  @Test
  public void rejectsChangedKind() {
    assertThrows(IOException.class, () -> parse("0,a,RED,1,0,0,1\n1000,a,BLUE,1,0,0,1"));
  }

  @Test
  public void rejectsDecreasingTimestamps() {
    assertThrows(IOException.class, () -> parse(VALID + "500,a,RED,1,0,0,1"));
  }

  @Test
  public void rejectsOverlongLines() {
    assertThrows(IOException.class, () -> parse("a".repeat(513)));
  }

  @Test
  public void rejectsTooManyEntities() {
    StringBuilder rows = new StringBuilder();
    for (int i = 0; i < 65; i++) rows.append("0,a").append(i).append(",RED,1,0,0,1\n");
    assertThrows(IOException.class, () -> parse(rows.toString()));
  }

  @Test
  public void rejectsEmptyAndSingleFrame() {
    assertThrows(IOException.class, () -> parse(""));
    assertThrows(IOException.class, () -> parse("0,a,RED,1,0,0,1"));
  }

  @Test
  public void bundledFixturesAreValid() throws Exception {
    for (Sport sport : Sport.values())
      try (Reader reader =
          Files.newBufferedReader(
              Paths.get("../app/src/main/assets/fixtures/" + sport.fixture),
              StandardCharsets.UTF_8)) {
        Replay replay = ReplayCsv.read(reader, sport);
        assertTrue(replay.durationMs() > 0);
        for (long t = 0; t <= replay.durationMs(); t += 125)
          assertFalse(replay.sample(t).entities.isEmpty());
      }
  }

  @Test
  public void callerRetainsReaderOwnership() throws Exception {
    boolean[] closed = {false};
    StringReader reader =
        new StringReader(ReplayCsv.HEADER + "\n" + VALID) {
          @Override
          public void close() {
            closed[0] = true;
          }
        };
    ReplayCsv.read(reader, Sport.WATER_POLO);
    assertFalse(closed[0]);
  }
}
