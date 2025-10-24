package com.jigmjugm.common.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ChallengePolicy {

    public String computeStatus(LocalDate start, LocalDate end, LocalDate today) {
        if (today.isBefore(start)) return "PENDING";
        if (!today.isAfter(end))   return "ACTIVE";
        return "COMPLETED";
    }

    // MON=1<<0 ... SUN=1<<6  -> mask 합산
    public int toWeeklyMask(List<String> weeklyDays) {
        if (weeklyDays == null || weeklyDays.isEmpty()) return 0;
        int mask = 0;
        for (String s : weeklyDays) {
            mask |= switch (s) {
                case "MON" -> 1;
                case "TUE" -> 2;
                case "WED" -> 4;
                case "THU" -> 8;
                case "FRI" -> 16;
                case "SAT" -> 32;
                case "SUN" -> 64;
                default -> 0;
            };
        }
        return mask;
    }

    public List<LocalDate> buildSchedule(String frequencyType, int weeklyMask,
                                         LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        if ("DAILY".equalsIgnoreCase(frequencyType)) {
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) dates.add(d);
        } else { // WEEKLY
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                int bit = 1 << (d.getDayOfWeek().getValue() - 1); // MON=1 .. SUN=64
                if ((weeklyMask & bit) != 0) dates.add(d);
            }
        }
        return dates;
    }
}

