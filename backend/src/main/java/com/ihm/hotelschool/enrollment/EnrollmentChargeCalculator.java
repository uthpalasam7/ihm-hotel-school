package com.ihm.hotelschool.enrollment;

import com.ihm.hotelschool.batch.FeePlan;
import com.ihm.hotelschool.enrollment.dto.EnrollmentPreview;
import com.ihm.hotelschool.enrollment.dto.EnrollmentPreview.Charge;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentChargeCalculator {
    public EnrollmentPreview calculate(FeePlan plan, LocalDate batchStart, LocalDate enrollmentDate) {
        int months = plan.getDurationMonths();
        int day = plan.getMonthlyDueDay();
        if (months < 1 || day < 1 || day > 31) {
            throw new IllegalArgumentException("Fee plan duration or monthly due day is invalid");
        }
        var charges = new ArrayList<Charge>();
        charges.add(new Charge("REGISTRATION_FEE", null, "Registration fee", enrollmentDate, plan.getRegistrationFee()));
        BigDecimal installment = plan.getCourseFee().divide(BigDecimal.valueOf(months), 2, RoundingMode.DOWN);
        for (int index = 0; index < months; index++) {
            YearMonth month = YearMonth.from(batchStart).plusMonths(index);
            LocalDate due = month.atDay(Math.min(day, month.lengthOfMonth()));
            if (index == 0 && due.isBefore(batchStart)) due = batchStart;
            BigDecimal amount = index == months - 1
                    ? plan.getCourseFee().subtract(installment.multiply(BigDecimal.valueOf(months - 1))) : installment;
            charges.add(new Charge("COURSE_INSTALLMENT", index + 1, "Course installment " + (index + 1), due, amount));
        }
        charges.add(new Charge("EXAMINATION_FEE", null, "Examination fee", plan.getExaminationDueDate(), plan.getExaminationFee()));
        return new EnrollmentPreview(plan.getCurrencyCode(), charges.stream().map(Charge::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add), List.copyOf(charges), plan.getBatch().getVersion(), plan.getVersion());
    }
}
