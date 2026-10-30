package edu.frederick.poolar.core;

public enum Sport {
  WATER_POLO("Water polo", "water-polo.csv", 30.0d, 20.0d),
  SWIMMING("Swimming", "swimming.csv", 25.0d, 16.0d);

  public final String fixture;
  public final double length;
  public final String title;
  public final double width;

  Sport(String title, String fixture, double length, double width) {
    this.title = title;
    this.fixture = fixture;
    this.length = length;
    this.width = width;
  }
}
