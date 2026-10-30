package edu.frederick.poolar.core;

import java.io.*;
import java.util.*;

/** Strict, bounded parser for the unquoted seven-column demo format. */
public final class ReplayCsv {
  public static final String HEADER = "time_ms,id,kind,x_m,y_m,z_m,confidence";
  public static final int MAX_ROWS = 100_000;
  public static final int MAX_ENTITIES = 64;
  public static final int MAX_LINE_LENGTH = 512;

  private ReplayCsv() {}

  public static Replay read(Reader source, Sport sport) throws IOException {
    if (source == null || sport == null)
      throw new IllegalArgumentException("Reader and sport required");
    // Caller owns the reader. Bound lines before allocating an unbounded String.
    BufferedReader input =
        source instanceof BufferedReader ? (BufferedReader) source : new BufferedReader(source);
    if (!HEADER.equals(line(input))) throw new IOException("Line 1: invalid CSV header");
    List<Frame> frames = new ArrayList<>();
    List<Entity> entities = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    Map<String, Entity.Kind> kinds = new HashMap<>();
    long current = -1;
    int count = 1;
    String row;
    while ((row = line(input)) != null) {
      count++;
      if (count > MAX_ROWS + 1) throw new IOException("Replay exceeds row limit");
      try {
        String[] cells = row.split(",", -1);
        if (cells.length != 7) throw new IllegalArgumentException("Expected seven columns");
        long time = Long.parseLong(cells[0]);
        if (time < 0 || time < current || time > 86_400_000)
          throw new IllegalArgumentException("Timestamps must increase within one day");
        if (current == -1 && time != 0)
          throw new IllegalArgumentException("First timestamp must be zero");
        if (time != current) {
          if (current >= 0) frames.add(new Frame(current, entities));
          entities = new ArrayList<>();
          ids.clear();
          current = time;
        }
        Entity entity =
            new Entity(
                cells[1],
                Entity.Kind.valueOf(cells[2]),
                Double.parseDouble(cells[3]),
                Double.parseDouble(cells[4]),
                Double.parseDouble(cells[5]),
                Double.parseDouble(cells[6]));
        if ((sport == Sport.SWIMMING) != (entity.kind == Entity.Kind.SWIMMER))
          throw new IllegalArgumentException("Entity kind does not match sport");
        if (entity.x < 0
            || entity.x > sport.length
            || entity.z < 0
            || entity.z > sport.width
            || entity.y < 0
            || entity.y > 5) throw new IllegalArgumentException("Position outside pool bounds");
        if (!ids.add(entity.id)) throw new IllegalArgumentException("Duplicate entity identifier");
        Entity.Kind previous = kinds.putIfAbsent(entity.id, entity.kind);
        if (previous != null && previous != entity.kind)
          throw new IllegalArgumentException("Entity changed kind");
        if (entities.size() >= MAX_ENTITIES)
          throw new IllegalArgumentException("Too many entities in a frame");
        if (entity.kind == Entity.Kind.BALL
            && entities.stream().anyMatch(e -> e.kind == Entity.Kind.BALL))
          throw new IllegalArgumentException("Only one ball is allowed");
        entities.add(entity);
      } catch (IllegalArgumentException failure) {
        throw new IOException("Line " + count + ": " + failure.getMessage(), failure);
      }
    }
    if (current >= 0) frames.add(new Frame(current, entities));
    try {
      return new Replay(sport, frames);
    } catch (IllegalArgumentException failure) {
      throw new IOException(failure.getMessage(), failure);
    }
  }

  private static String line(Reader input) throws IOException {
    StringBuilder value = new StringBuilder();
    int c;
    while ((c = input.read()) != -1) {
      if (c == '\n') break;
      if (value.length() >= MAX_LINE_LENGTH) throw new IOException("CSV line exceeds length limit");
      value.append((char) c);
    }
    if (c == -1 && value.length() == 0) return null;
    if (value.length() > 0 && value.charAt(value.length() - 1) == '\r')
      value.setLength(value.length() - 1);
    return value.toString();
  }
}
