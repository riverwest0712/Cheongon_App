package com.example.cheongon.recommendation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cheongon.R;
import com.example.cheongon.main.BottomMenu;
import com.example.cheongon.main.Hamburger;
import com.example.cheongon.main.TopHeader;

import java.text.DecimalFormat;

public class IncomeCalculatorActivity extends AppCompatActivity {
    public static final String EXTRA_MONTHLY_INCOME = "monthly_income";

    // 한국장학재단 공개 산정 기준(만원 단위)
    private static final double BASIC_PROPERTY_DEDUCTION = 6900.0;
    private static final double STUDENT_INCOME_DEDUCTION = 130.0;
    private static final double GENERAL_MONTHLY_RATE = 0.0417 / 3.0;
    private static final double FINANCIAL_MONTHLY_RATE = 0.0626 / 3.0;
    private static final double SIBLING_DEDUCTION_PER_PERSON = 40.0;

    private final DecimalFormat amountFormat = new DecimalFormat("#,##0.##");
    private double calculatedMonthlyIncome;
    private Hamburger hamburger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_calculator);

        hamburger = new Hamburger(this);
        new TopHeader(this);
        new BottomMenu(this);

        findViewById(R.id.btnBack).setOnClickListener(view -> finish());

        Spinner siblingSpinner = findViewById(R.id.spinnerSiblingCount);
        ArrayAdapter<String> siblingAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"1명", "2명", "3명", "4명", "5명", "6명 이상"});
        siblingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        siblingSpinner.setAdapter(siblingAdapter);

        findViewById(R.id.btnCalculateIncome).setOnClickListener(view -> calculate());
        findViewById(R.id.btnUseCalculatedIncome).setOnClickListener(view -> returnResult());
    }

    private void calculate() {
        double studentIncome = value(R.id.etStudentIncome);
        double incomeEvaluation = Math.max(0, studentIncome - STUDENT_INCOME_DEDUCTION)
                + value(R.id.etFamilyWorkIncome)
                + value(R.id.etBusinessIncome)
                + value(R.id.etPropertyIncome)
                + value(R.id.etOtherIncome);

        double generalProperty = value(R.id.etRealEstate)
                + value(R.id.etLeaseDeposit)
                + value(R.id.etOtherProperty);
        double financialProperty = value(R.id.etFinancialProperty);
        double debt = value(R.id.etFinancialDebt)
                + value(R.id.etStudentLoanDebt)
                + value(R.id.etOtherDebt);

        // 기본재산액과 부채는 일반재산에서 먼저, 남으면 금융재산에서 차감한다.
        double totalDeduction = BASIC_PROPERTY_DEDUCTION + debt;
        double netGeneralProperty = Math.max(0, generalProperty - totalDeduction);
        double remainingDeduction = Math.max(0, totalDeduction - generalProperty);
        double netFinancialProperty = Math.max(0, financialProperty - remainingDeduction);

        CheckBox disabledVehicle = findViewById(R.id.cbDisabledVehicle);
        double vehicle = disabledVehicle.isChecked() ? 0 : value(R.id.etVehicleValue);
        double propertyConversion = netGeneralProperty * GENERAL_MONTHLY_RATE
                + netFinancialProperty * FINANCIAL_MONTHLY_RATE
                + vehicle * GENERAL_MONTHLY_RATE;

        Spinner siblingSpinner = findViewById(R.id.spinnerSiblingCount);
        int siblingCount = siblingSpinner.getSelectedItemPosition() + 1;
        RadioButton unmarried = findViewById(R.id.rbUnmarried);
        double siblingDeduction = unmarried.isChecked() && siblingCount >= 3
                ? (siblingCount - 2) * SIBLING_DEDUCTION_PER_PERSON : 0;

        calculatedMonthlyIncome = Math.max(0,
                incomeEvaluation + propertyConversion - siblingDeduction);

        setAmount(R.id.tvRecognizedMonthlyIncome, calculatedMonthlyIncome + 0.0, "");
        setAmount(R.id.tvIncomeEvaluationResult, incomeEvaluation, "월 소득평가액 ");
        setAmount(R.id.tvPropertyConversionResult, propertyConversion, "재산의 월 소득환산액 ");
        setAmount(R.id.tvSiblingDeductionResult, siblingDeduction, "형제·자매 공제액 -");
        findViewById(R.id.layoutIncomeResult).setVisibility(View.VISIBLE);
    }

    private double value(int viewId) {
        String text = ((EditText) findViewById(viewId)).getText().toString()
                .replace(",", "").trim();
        if (text.isEmpty()) return 0;
        try {
            return Math.max(0, Double.parseDouble(text));
        } catch (NumberFormatException ignored) {
            ((EditText) findViewById(viewId)).setError("숫자로 입력해주세요.");
            return 0;
        }
    }

    private void setAmount(int viewId, double amount, String prefix) {
        ((TextView) findViewById(viewId)).setText(prefix + amountFormat.format(amount) + "만원");
    }

    private void returnResult() {
        Intent result = new Intent();
        result.putExtra(EXTRA_MONTHLY_INCOME, (int) Math.round(calculatedMonthlyIncome));
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hamburger != null) hamburger.updateLoginState();
    }
}
