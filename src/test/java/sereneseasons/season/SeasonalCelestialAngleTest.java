package sereneseasons.season;

import org.junit.jupiter.api.Test;
import sereneseasons.api.season.Season;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeasonalCelestialAngleTest
{
    @Test
    void springUsesReferenceSunriseAndSunset()
    {
        assertEquals(0.757F, SeasonalCelestialAngle.calculate(Season.SPRING, 0L, 0.0F), 0.0001F);
        assertEquals(0.2425F, SeasonalCelestialAngle.calculate(Season.SPRING, 14500L, 0.0F), 0.0001F);
    }

    @Test
    void autumnUsesReferenceSunriseAndSunset()
    {
        assertEquals(0.757F, SeasonalCelestialAngle.calculate(Season.AUTUMN, 1000L, 0.0F), 0.0001F);
        assertEquals(0.2425F, SeasonalCelestialAngle.calculate(Season.AUTUMN, 13000L, 0.0F), 0.0001F);
    }

    @Test
    void summerUsesReferenceSunriseAndSunset()
    {
        assertEquals(0.757F, SeasonalCelestialAngle.calculate(Season.SUMMER, 23000L, 0.0F), 0.0001F);
        assertEquals(0.2425F, SeasonalCelestialAngle.calculate(Season.SUMMER, 15500L, 0.0F), 0.0001F);
    }

    @Test
    void winterUsesReferenceSunriseAndSunset()
    {
        assertEquals(0.757F, SeasonalCelestialAngle.calculate(Season.WINTER, 2000L, 0.0F), 0.0001F);
        assertEquals(0.2425F, SeasonalCelestialAngle.calculate(Season.WINTER, 10500L, 0.0F), 0.0001F);
    }
}
