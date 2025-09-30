package M2;

public class Problem4 extends BaseClass {
    private static String[] array1 = {
        "hello world!",
        "java programming",
        "special@#$%^&characters",
        "numbers 123 456",
        "mIxEd CaSe InPut!"
    };

    private static String[] array2 = {
        "hello world",
        "java programming",
        "this is a title case test",
        "capitalize every word",
        "mixEd CASE input"
    };

    private static String[] array3 = {
        "  hello   world  ",
        "java    programming  ",
        "  extra    spaces  between   words   ",
        "      leading and trailing spaces      ",
        "multiple      spaces"
    };

    private static String[] array4 = {
        "hello world",
        "java programming",
        "short",
        "a",
        "even"
    };

    private static void transformText(String[] arr, int arrayNumber) {
        // Only make edits between the designated "Start" and "End" comments
        printArrayInfoBasic(arr, arrayNumber);

        String placeholderForModifiedPhrase = "";
        String placeholderForMiddleCharacters = "";

        for (int i = 0; i < arr.length; i++) {
            // Start Solution Edits
            // UCID: st944 | Name: Sahith T. | Date: 09/29/2025
            // Plan:
            // 1. Remove non-alphanumeric characters except spaces
            // 2. Convert to Title Case
            // 3. Trim spaces and collapse multiple spaces
            // 4. Assign result to placeholderForModifiedPhrase
            // Extra Credit:
            // 5. If >= 3 chars, extract middle 3 characters
            // 6. Otherwise, assign "Not enough characters"

            String phrase = arr[i];

            // Challenge 1: remove non-alphanumeric except spaces
            String cleaned = phrase.replaceAll("[^a-zA-Z0-9 ]", "");

            // Challenge 2: convert to Title Case
            String[] words = cleaned.trim().toLowerCase().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (w.length() > 0) {
                    sb.append(Character.toUpperCase(w.charAt(0)))
                      .append(w.substring(1))
                      .append(" ");
                }
            }

            // Challenge 3: trim & collapse spaces
            placeholderForModifiedPhrase = sb.toString().trim();

            // Challenge 4: extra credit – middle 3 characters
            if (placeholderForModifiedPhrase.length() >= 3) {
                int mid = placeholderForModifiedPhrase.length() / 2;
                int start = Math.max(0, mid - 1);
                int end = Math.min(placeholderForModifiedPhrase.length(), mid + 2);
                placeholderForMiddleCharacters = placeholderForModifiedPhrase.substring(start, end);
            } else {
                placeholderForMiddleCharacters = "Not enough characters";
            }
            // End Solution Edits

            System.out.println(String.format(
                "Index[%d] \"%s\" | Middle: \"%s\"",
                i, placeholderForModifiedPhrase, placeholderForMiddleCharacters
            ));
        }

        System.out.println("\n______________________________________");
    }

    public static void main(String[] args) {
        final String ucid = "st944"; // <-- updated to your UCID
        // No edits below this line
        printHeader(ucid, 4);

        transformText(array1, 1);
        transformText(array2, 2);
        transformText(array3, 3);
        transformText(array4, 4);

        printFooter(ucid, 4);
    }
}
