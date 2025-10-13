package M3;

/*
UCID: st944
Date: 10/13/2025
Summary: SlashCommandHandler accepts user input as slash commands and performs actions like
greet, roll, and echo. It validates formats, handles errors, and exits on /quit.
*/

import java.util.Scanner;
import java.util.Random;

public class SlashCommandHandler extends BaseClass {
    private static String ucid = "st944"; // <-- your UCID

    public static void main(String[] args) {
        printHeader(ucid, 2, "Objective: Implement a simple slash command parser.");
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        while (true) {
            System.out.print("Enter command: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("/quit")) {
                System.out.println("Exiting program. Goodbye!");
                break;
            }

            // /greet <name>
            else if (input.toLowerCase().startsWith("/greet")) {
                String[] parts = input.split(" ", 2);
                if (parts.length == 2 && !parts[1].isEmpty()) {
                    System.out.println("Hello, " + parts[1] + "!");
                } else {
                    System.out.println("Error: Usage: /greet <name>");
                }
            }

            // /roll <num>d<sides>
            else if (input.toLowerCase().startsWith("/roll")) {
                String[] parts = input.split(" ", 2);
                if (parts.length != 2) {
                    System.out.println("Error: Usage: /roll <num>d<sides>");
                    continue;
                }
                try {
                    String[] rollParts = parts[1].toLowerCase().split("d");
                    int num = Integer.parseInt(rollParts[0]);
                    int sides = Integer.parseInt(rollParts[1]);
                    if (num <= 0 || sides <= 0) {
                        System.out.println("Error: Both values must be positive integers.");
                        continue;
                    }
                    int total = 0;
                    for (int i = 0; i < num; i++) {
                        total += random.nextInt(sides) + 1;
                    }
                    System.out.println("Rolled " + num + "d" + sides + " and got " + total + "!");
                } catch (Exception e) {
                    System.out.println("Error: Invalid format. Usage: /roll <num>d<sides>");
                }
            }

            // /echo <message>
            else if (input.toLowerCase().startsWith("/echo")) {
                String[] parts = input.split(" ", 2);
                if (parts.length == 2 && !parts[1].isEmpty()) {
                    System.out.println(parts[1]);
                } else {
                    System.out.println("Error: Usage: /echo <message>");
                }
            }

            // Unrecognized command
            else {
                System.out.println("Error: Unrecognized command. Try /greet, /roll, /echo, or /quit.");
            }
        }

        printFooter(ucid, 2);
        scanner.close();
    }
}
