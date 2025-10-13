package M3;

/*
UCID: st944
Date: 10/13/2025
Summary: CommandLineCalculator accepts two numbers and an operator (+ or -)
from the command line, performs the calculation, and prints the correctly
formatted result using the maximum decimal precision found in the inputs.
*/

public class CommandLineCalculator extends BaseClass {
    private static String ucid = "st944"; // <-- UCID variable updated

    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

        // --- Validate argument count ---
        // UCID: st944 | Name: Sahith T. | Date: 09/29/2025

        if (args.length != 3) {
            System.out.println("Usage: java M3.CommandLineCalculator <num1> <operator> <num2>");
            printFooter(ucid, 1);
            return;
        }

        try {
            System.out.println("Calculating result...");

            String num1Str = args[0];
            String operator = args[1];
            String num2Str = args[2];

            double num1 = Double.parseDouble(num1Str);
            double num2 = Double.parseDouble(num2Str);

            double result;

            // --- Perform operation (+ or - only) ---
            if (operator.equals("+")) {
                result = num1 + num2;
            } else if (operator.equals("-")) {
                result = num1 - num2;
            } else {
                System.out.println("Error: Unsupported operator. Use + or - only.");
                printFooter(ucid, 1);
                return;
            }

            // --- Determine maximum decimal precision ---
            int decimals = Math.max(getDecimals(num1Str), getDecimals(num2Str));

            // --- Display formatted result with matching precision ---
            System.out.printf("%s %s %s = %." + decimals + "f%n", num1Str, operator, num2Str, result);

        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format.");
        } catch (Exception e) {
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
        }

        printFooter(ucid, 1);
    }

    // Helper method: count decimal places in a numeric string
    private static int getDecimals(String num) {
        if (num.contains(".")) {
            return num.length() - num.indexOf('.') - 1;
        }
        return 0;
    }
}
