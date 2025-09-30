package M3;

/*
UCID: st944
Date: 09/30/2025
Summary: CommandLineCalculator accepts two numbers and an operator (+ or -) 
from the command-line, performs the calculation, and formats the result 
to preserve the maximum number of decimal places from the inputs.
*/

public class CommandLineCalculator extends BaseClass {
    private static String ucid = "st944"; // <-- your UCID

    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

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
            if (operator.equals("+")) {
                result = num1 + num2;
            } else if (operator.equals("-")) {
                result = num1 - num2;
            } else {
                System.out.println("Error: Unsupported operator. Use + or - only.");
                printFooter(ucid, 1);
                return;
            }

            // Determine max decimal places from inputs
            int decimals = Math.max(getDecimals(num1Str), getDecimals(num2Str));

            // Print formatted result
            System.out.printf("%." + decimals + "f%n", result);

        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format.");
        } catch (Exception e) {
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
        }

        printFooter(ucid, 1);
    }

    // Helper method: count decimal places in a string number
    private static int getDecimals(String num) {
        if (num.contains(".")) {
            return num.length() - num.indexOf('.') - 1;
        }
        return 0;
    }
}
