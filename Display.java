// images
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
// Files
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
// misc
import java.util.Scanner;

public class Display {
    public static void main(String[] args) throws InterruptedException {
        Scanner scan = new Scanner(System.in);
        Path folderPath = Paths.get("D:/");
        Path selectedImageFile = null;
        System.out.println("=== SELECT IMAGE ===");

        // file explorer sim
        while (selectedImageFile == null) {
            List<Path> currentItems = new ArrayList<>();

            System.out.println("\nCurrent Location: " + (folderPath == null ? "This PC" : folderPath.toAbsolutePath()));
            System.out.println("[0] Back");

            if (folderPath == null) {
                File[] roots = File.listRoots();
                for (File root : roots) {
                    currentItems.add(root.toPath());
                }
            } else {
                try (Stream<Path> stream = Files.list(folderPath)) {
                    currentItems = stream.collect(Collectors.toList());
                } catch (IOException e) {
                    System.err.println("Error listing! Moving back to start.");
                    folderPath = null;
                    continue;
                }
            }

            // sort
            List<Path> files = new ArrayList<>();
            for (int i = currentItems.size()-1; i >= 0; i--) {
                Path item = currentItems.get(i);
                if (!Files.isDirectory(item)) {
                    files.add(item);
                    currentItems.remove(i);
                }
            }
            for (int i = 0; i < files.size(); i++) {
                Path item = files.get(i);
                currentItems.add(item);
            }

            // display
            for (int i = 0; i < currentItems.size(); i++) {
                Path item = currentItems.get(i);
                String prefix = "("+ (Files.isDirectory(item)? "FOLD":"FILE")+") ";
                System.out.println("["+(i+1)+"] "+prefix+item.getFileName());
            }
            System.out.print("Select direction: ");
            if (!scan.hasNextInt()) {
                scan.next();
                continue;
            }
            int inp = scan.nextInt();
            if (inp == 0) {
                // Go back
                if (folderPath == null) {
                    System.out.println("can't go back :(");
                } else {
                    folderPath = folderPath.getParent();
                }
            } else if (inp > 0 && inp <= currentItems.size()) {
                Path chosen = currentItems.get(inp - 1);
                if (Files.isDirectory(chosen)) folderPath = chosen;
                else selectedImageFile = chosen;
            } else {
                System.out.println("Invalid choice!");
            }
        }
        try {
            System.out.print("\nMedia Width: ");
            int targetWidth = 1;
            while (true) {
                if (!scan.hasNextInt()) {
                    scan.next();
                    continue;
                }
                targetWidth = scan.nextInt();
                if (targetWidth <= 0) {
                    System.out.println("Invalid choice!");
                } else break;
            }
            
            File file = selectedImageFile.toFile();
            BufferedImage originalImage = ImageIO.read(file);

            double aspectRatio = (double) originalImage.getHeight() / originalImage.getWidth();
            int targetHeight = (int) (targetWidth * aspectRatio / 1.25); 

            // compress
            Image scaledImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            resizedImage.getGraphics().drawImage(scaledImage, 0, 0, null);

            // displaying
            StringBuilder output = new StringBuilder();
            for (int y = 0; y < targetHeight; y++) {
                for (int x = 0; x < targetWidth; x++) {
                    int pixel = resizedImage.getRGB(x, y);
                    int red   = (pixel >> 16) & 0xff;
                    int green = (pixel >> 8)  & 0xff;
                    int blue  =  pixel        & 0xff;

                    output.append(String.format("\033[38;2;%d;%d;%dm██", red, green, blue));
                }
                output.append("\033[0m\n");
            }

            int speed = (int)Math.round(output.length() / 1000);
            for (int i = 0;i<output.length();i++) {
                System.out.print(output.toString().charAt(i));
                if (i % speed == 0) Thread.sleep(1);
            }
        } catch (IOException e) {
            System.err.println("Error reading the image file: " + e.getMessage());
        }
        scan.close();
    }
}