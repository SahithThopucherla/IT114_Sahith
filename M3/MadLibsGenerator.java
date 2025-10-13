package M3;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/*
UCID: st944
Date: 10/13/2025
Summary: The MadLibsGenerator randomly loads a story file from the "stories" folder,
prompts the user to fill in placeholders (like <noun> or <adjective>), replaces them
with the user’s responses, and displays the completed story.
*/

public class MadLibsGenerator extends BaseClass {
    private static final String STORIES_FOLDER = "M3/stories";
    private static String ucid = "st944";

    public static void main(String[] args) {
        printHeader(ucid, 3, "Objective: Implement a Mad Libs generator that replaces placeholders dynamically.");
        Scanner scanner = new Scanner(System.in);
        File folder = new File(STORIES_FOLDER);

        // Check that the folder exists and contains stories
        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length == 0) {
            System.out.println("Error: No stories found in the 'stories' folder.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }

        // Step 1: Load a random story file
        File[] storyFiles = folder.listFiles();
        Random random = new Random();
        File storyFile = storyFiles[random.nextInt(storyFiles.length)];
        System.out.println("Selected story: " + storyFile.getName());

        List<String> lines = new ArrayList<>();

        //  Step 2: Read all lines from the selected story
        try (Scanner fileScanner = new Scanner(storyFile)) {
            while (fileScanner.hasNextLine()) {
                lines.add(fileScanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error: Could not read the story file.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }

        //  Step 3: Iterate through each line and replace placeholders
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Continue replacing placeholders until none remain
            while (line.contains("<") && line.contains(">")) {
                int start = line.indexOf("<");
                int end = line.indexOf(">", start);
                if (end == -1) break; // malformed placeholder

                String placeholder = line.substring(start + 1, end);

                // Display placeholder with spaces instead of underscores
                String displayPrompt = placeholder.replace("_", " ");
                System.out.print("Enter a(n) " + displayPrompt + ": ");
                String userWord = scanner.nextLine();

                // Replace only the first occurrence
                line = line.substring(0, start) + userWord + line.substring(end + 1);
            }

            // Update the modified line in the same position
            lines.set(i, line);
        }

        //  Step 4: Display the completed story
        System.out.println("\nYour Completed Mad Libs Story:\n");
        for (String line : lines) {
            System.out.println(line);
        }

        printFooter(ucid, 3);
        scanner.close();
    }
}
