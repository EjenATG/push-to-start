// misc
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// files
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// XML parser
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

// XML transformer
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

// XML XPath
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

// XML DOM
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class VectorDrawer {
    static class PathInstruction {
        char type;
        double x, y;
        int colour;
        int shapeId;

        PathInstruction(char type, double x, double y, int colour, int shapeId) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.colour = colour;
            this.shapeId = shapeId;
        }
    }

    public static void main(String[] args) throws Exception {
        List<Path> currentItems = new ArrayList<>();
        Scanner inp = new Scanner(System.in);
        File vectorFile = null; 
        Document doc = null;

        // vector file
        try (Stream<Path> stream = Files.list(Path.of("media"))) {
            currentItems = stream.filter(path -> {
                if (!Files.isRegularFile(path)) return false;
                String name = path.getFileName().toString().toLowerCase();
                return name.endsWith(".svg") || name.endsWith(".eps") || name.endsWith(".ai");
            }).collect(Collectors.toList());
        } catch (IOException e) { System.err.println("Error listing media directory!"); }
        if (currentItems.isEmpty()) { System.out.println("No supported vector files found :("); inp.close(); return; }
        while (true) {
            System.out.println("Select your vector file:");
            for (int i = 0; i < currentItems.size(); i++) System.out.println("["+(i+1)+"] " + currentItems.get(i).getFileName());
            System.out.print("Select choice: ");
            if (!inp.hasNextInt()) { inp.next(); continue; }
            int num = inp.nextInt();
            if (num > 0 && num <= currentItems.size()) { vectorFile = currentItems.get(num-1).toFile(); break; }
            else System.out.println("Invalid choice!");
        }
        System.out.println("Picked: " + vectorFile.getName());

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(vectorFile);
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            e.printStackTrace();
            inp.close();
            return;
        }
        System.out.print("Grid size: ");
        int gridSize = inp.nextInt();
        int[][] grid = new int[gridSize][gridSize];
        for (int[] row : grid) Arrays.fill(row, -1);
        List<PathInstruction> instructions = parseFile(doc, gridSize);

        editor: while (true) {
            drawVector(instructions, vectorFile, gridSize, grid);
            System.out.println("Select choice: \n[0] Save as new File\n[1] Recolour\n[2] Rotate");
            int choice = inp.nextInt();

            Element selectedPath = null;
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.compile("//path").evaluate(doc, XPathConstants.NODESET);
            if (choice != 0) {
                System.out.println("Select shape:");
                System.out.println("[0] Everything");

                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element path = (Element) nodeList.item(i);
                    String hex = path.getAttribute("fill");
                    int colour = Integer.parseInt(hex.substring(1),16);
                    int red = (colour >> 16) & 0xff;
                    int green = (colour >> 8) & 0xff;
                    int blue = colour & 0xff;

                    System.out.println("["+(i+1)+"] "+ String.format("\033[48;2;%d;%d;%dm   \033[0m", red, green, blue)+ " " + hex);
                }
                System.out.print("Select shape: ");

                int shapeChoice = inp.nextInt();
                inp.nextLine();
                if (shapeChoice < 0 || shapeChoice > nodeList.getLength()) {
                    System.out.println("Invalid shape!");
                    break;
                }
                if (shapeChoice > 0) selectedPath = (Element)nodeList.item(shapeChoice - 1);
            }

            switch (choice) {
                case 0:
                    // save setup
                    String name = vectorFile.getName();
                    String base = name.substring(0, name.lastIndexOf('.'));
                    String extension = name.substring(name.lastIndexOf('.'));

                    int number = 1;
                    File output;
                    do {
                        output = new File(vectorFile.getParent(), base + " (" + number + ")" + extension);
                        number++;
                    } while (output.exists());

                    TransformerFactory tFactory = TransformerFactory.newInstance();
                    Transformer transformer = tFactory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty("{http://apache.org}indent-amount", "4");
                    
                    DOMSource source = new DOMSource(doc);
                    StreamResult result = new StreamResult(output);
                    transformer.transform(source, result);

                    System.out.println("Saved file as: " + output.getName() + "\nMake sure to Convert to Shape after import!");
                    inp.close();
                    break editor;
                case 1:
                    colorEdit: while (true) {
                        String hex = selectedPath.getAttribute("fill");

                        int colour = Integer.parseInt(hex.substring(1), 16);
                        int r = (colour >> 16) & 0xff;
                        int g = (colour >> 8) & 0xff;
                        int b = colour & 0xff;
                        System.out.println("Edit colour:\n[r,g,b + value(0-255)]\nExample: 'g30'\nType any exception to finish!");
                        String edit = inp.nextLine();
                        if (edit.isEmpty()) continue;
                        else if ("rgb".indexOf(edit.charAt(0)) == -1) break colorEdit;
                        try {
                            int value = Integer.parseInt(edit.substring(1));
                            if (value < 0 || value > 255) {
                                System.out.println("Colour value must be 0-255!");
                                continue;
                            }
                            switch (edit.charAt(0)) {
                                case 'r':
                                    r = value;
                                    break;
                                case 'g':
                                    g = value;
                                    break;
                                case 'b':
                                    b = value;
                                    break;
                            }
                            boolean all = true;
                            if (selectedPath != null) {
                                selectedPath.setAttribute("fill", String.format("#%02X%02X%02X", r, g, b));
                                all = false;
                            }
                            if (all) for (int i = 0;i<nodeList.getLength();i++) {
                                Element shape = (Element)nodeList.item(i);
                                shape.setAttribute("fill", String.format("#%02X%02X%02X", r, g, b));
                            }
                            instructions = parseFile(doc, gridSize);
                            drawVector(instructions, vectorFile, gridSize, grid);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            System.exit(0);
                        }
                    }
                    break;
                case 2:
                    rotateEdit: while (true) {
                        System.out.print("Type non-number to finish.\nRotate clockwise by: ");
                        String input = inp.nextLine();
                        double angle;
                        try {
                            angle = Double.parseDouble(input);
                        } catch (NumberFormatException e) {
                            break rotateEdit;
                        }

                        boolean all = true;
                        double minX = Double.MAX_VALUE;
                        double maxX = -Double.MAX_VALUE;
                        double minY = Double.MAX_VALUE;
                        double maxY = -Double.MAX_VALUE;
                        if (selectedPath != null) {
                            int shapeId = -1;
                            for (int i = 0; i < nodeList.getLength(); i++) {
                                if (nodeList.item(i) == selectedPath) {
                                    shapeId = i;
                                    break;
                                }
                            }

                            List<PathInstruction> shape = new ArrayList<>();
                            for (PathInstruction inst : instructions) {
                                if (inst.shapeId == shapeId) {
                                    shape.add(inst);
                                }
                            }

                            for (PathInstruction inst : shape) {
                                if (inst.type == 'Z') continue;
                                minX = Math.min(minX, inst.x);
                                maxX = Math.max(maxX, inst.x);
                                minY = Math.min(minY, inst.y);
                                maxY = Math.max(maxY, inst.y);
                            }
                            double centreX = (minX + maxX) / 2.0;
                            double centreY = (minY + maxY) / 2.0;
                            String transform = selectedPath.getAttribute("transform");

                            double oldAngle = 0;

                            if (!transform.isEmpty() && transform.startsWith("rotate(")) {
                                String values = transform.substring(7, transform.length() - 1);
                                oldAngle = Double.parseDouble(values.trim().split("\\s+")[0]);
                            }

                            double newAngle = oldAngle + angle;

                            selectedPath.setAttribute("transform",String.format("rotate(%f %.6f %.6f)",newAngle,centreX,centreY));
                            all = false;
                        }
                        if (all) {
                            minX = Double.MAX_VALUE;
                            maxX = -Double.MAX_VALUE;
                            minY = Double.MAX_VALUE;
                            maxY = -Double.MAX_VALUE;
                            for (PathInstruction inst : instructions) {
                                if (inst.type == 'Z') continue;
                                minX = Math.min(minX, inst.x);
                                maxX = Math.max(maxX, inst.x);
                                minY = Math.min(minY, inst.y);
                                maxY = Math.max(maxY, inst.y);
                            }

                            double centreX = (minX + maxX) / 2.0;
                            double centreY = (minY + maxY) / 2.0;

                            for (int i = 0; i < nodeList.getLength(); i++) {
                                Element shapeElement = (Element) nodeList.item(i);
                                String transform = shapeElement.getAttribute("transform");

                                double oldAngle = 0;
                                if (!transform.isEmpty() && transform.startsWith("rotate(")) {
                                    String values = transform.substring(7, transform.length() - 1);
                                    oldAngle = Double.parseDouble(values.trim().split("\\s+")[0]);
                                }
                                double newAngle = oldAngle + angle;
                                shapeElement.setAttribute("transform",String.format("rotate(%f %.6f %.6f)",newAngle,centreX,centreY));
                            }
                        }
                        instructions = parseFile(doc, gridSize);
                        drawVector(instructions, vectorFile, gridSize, grid);
                    }
                    break;
                default:
                    System.out.println("Invalid choice");
                    break;
            }
        }
        inp.close();
    }

    private static void drawVector(List<PathInstruction> instRef, File vectorFile, int gridSize, int[][] grid) {
        // cleanup
        for (int y = 0; y < gridSize; y++) {
            Arrays.fill(grid[y], -1);
        }
        List<PathInstruction> instructions = new ArrayList<>();

        for (PathInstruction inst : instRef) {
            instructions.add(new PathInstruction(inst.type,inst.x,inst.y,inst.colour,inst.shapeId));
        }
        try {
            // compress
            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

            for (PathInstruction inst : instructions) {
                if (inst.type == 'Z') continue;

                minX = Math.min(minX, inst.x);
                maxX = Math.max(maxX, inst.x);
                minY = Math.min(minY, inst.y);
                maxY = Math.max(maxY, inst.y);
            }

            double shapeWidth = maxX - minX;
            double shapeHeight = maxY - minY;
            double targetSize = gridSize * 0.8;

            double scale = Math.min(targetSize / shapeWidth, targetSize / shapeHeight);
            shapeWidth *= scale;
            shapeHeight *= scale;
            double offsetX = (gridSize - shapeWidth) / 2.0;
            double offsetY = (gridSize - shapeHeight) / 2.0;

            for (PathInstruction inst : instructions) {
                if (inst.type == 'Z') continue;

                inst.x = (inst.x - minX) * scale + offsetX;
                inst.y = (inst.y - minY) * scale + offsetY;
            }

            // fill
            int shapeCount = 0;
            for (PathInstruction inst : instructions) shapeCount = Math.max(shapeCount, inst.shapeId + 1);

            for (int shapeId = 0; shapeId < shapeCount; shapeId++) {
                for (int y = 0; y < gridSize; y++) {
                    List<Double> intersections = new ArrayList<>();
                    PathInstruction prevPoint = null;
                    PathInstruction startOfContour = null;
                    List<PathInstruction> shape = new ArrayList<>();
                    for (PathInstruction inst : instructions)
                        if (inst.shapeId == shapeId) shape.add(inst);
                    if(shape.isEmpty()) continue;
                    int shapeColour = shape.get(0).colour;

                    for (PathInstruction inst : shape) {
                        if (inst.type == 'M') {
                            if (prevPoint != null && startOfContour != null && prevPoint != startOfContour) 
                                addIntersection(intersections, prevPoint, startOfContour, y);
                            startOfContour = inst;
                            prevPoint = inst;
                        } else if (inst.type == 'L') {
                            if (prevPoint != null) {
                                addIntersection(intersections, prevPoint, inst, y);
                            }
                            prevPoint = inst;
                        } else if (inst.type == 'Z') {
                            if (prevPoint != null && startOfContour != null) {
                                addIntersection(intersections, prevPoint, startOfContour, y);
                            }
                            prevPoint = startOfContour;
                        }
                    }
                    if (prevPoint != null && startOfContour != null && prevPoint != startOfContour) 
                        addIntersection(intersections, prevPoint, startOfContour, y);

                    java.util.Collections.sort(intersections);
                    for (int i = 0; i < intersections.size() - 1; i += 2) {
                        int startX = (int) Math.round(intersections.get(i));
                        int endX = (int) Math.round(intersections.get(i + 1));

                        startX = Math.max(0, Math.min(gridSize - 1, startX));
                        endX = Math.max(0, Math.min(gridSize - 1, endX));

                        for (int x = startX; x <= endX; x++) {
                            grid[y][x] = shapeColour; 
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        // display
        System.out.print("\033[H\033[2J\033[3J");
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                int pixel = grid[y][x];
                if (pixel == -1) {
                    System.out.print((x%2==0)?"   ":"  ");
                    continue;
                }
                int red   = (pixel >> 16) & 0xff;
                int green = (pixel >> 8)  & 0xff;
                int blue  =  pixel        & 0xff;
                System.out.print(String.format("\033[38;2;%d;%d;%dm"+(x%2==0?"███":"██"), red, green, blue));
            }
            System.out.println("\033[0m");
        }
    }

    private static List<PathInstruction> parseFile(Document doc, int gridSize) {
        List<PathInstruction> instructions = new ArrayList<>();
        
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "//path"; 
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);
                String pathData = element.getAttribute("d");
                int colour = Integer.parseInt(element.getAttribute("fill").substring(1),16);
                if (pathData == null || pathData.isEmpty()) continue;

                List<PathInstruction> pathInstructions = new ArrayList<>();

                Pattern tokenPattern = Pattern.compile("([a-zA-Z])|(-?\\d+\\.?\\d*)");
                Matcher matcher = tokenPattern.matcher(pathData);

                char currentCommand = 'M';
                List<Double> coords = new ArrayList<>();
                double currentX = 0, currentY = 0;
                double startX = 0, startY = 0;

                while (matcher.find()) {
                    if (matcher.group(1) != null) { 
                        processCommand(currentCommand, coords, pathInstructions, currentX, currentY, startX, startY, gridSize, colour, i);
                        if (!pathInstructions.isEmpty()) {
                            PathInstruction last = pathInstructions.get(pathInstructions.size() - 1);
                            currentX = last.x; currentY = last.y;
                        }
                        currentCommand = matcher.group(1).charAt(0);
                        coords.clear();
                        
                        if (currentCommand == 'Z' || currentCommand == 'z') {
                            pathInstructions.add(new PathInstruction('Z', startX, startY, colour, i));
                            currentX = startX; currentY = startY;
                        }
                    } else if (matcher.group(2) != null) { 
                        coords.add(Double.parseDouble(matcher.group(2)));
                    }
                }
                processCommand(currentCommand, coords, pathInstructions, currentX, currentY, startX, startY, gridSize, colour, i);

                String transform = element.getAttribute("transform");

                if (!transform.isEmpty() && transform.startsWith("rotate(")) {
                    int end = transform.indexOf(')', 7);

                    String values = transform.substring(7, end);
                    String[] tokens = values.trim().split("\\s+");

                    if (tokens.length >= 3) {
                        double rotateAngle = Double.parseDouble(tokens[0]);
                        double pivotX = Double.parseDouble(tokens[1]);
                        double pivotY = Double.parseDouble(tokens[2]);

                        double rad = Math.toRadians(rotateAngle);
                        double cos = Math.cos(rad);
                        double sin = Math.sin(rad);

                        for (PathInstruction inst : pathInstructions) {
                            if (inst.type == 'Z') continue;

                            double x = inst.x - pivotX;
                            double y = inst.y - pivotY;

                            inst.x = (x * cos - y * sin) + pivotX;
                            inst.y = (x * sin + y * cos) + pivotY;
                        }
                    }
                }
                instructions.addAll(pathInstructions);
            }
            return instructions;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return null;
    }

    private static void processCommand(char cmd, List<Double> coords, List<PathInstruction> instructions, double curX, double curY, double startX, double startY, int gridSize, int colour, int id) {
        if (coords.isEmpty()) return;

        int idx = 0;
        switch (cmd) {
            case 'M': // start of shape
            case 'm': 
                startX = coords.get(0); startY = coords.get(1);
                instructions.add(new PathInstruction('M', startX, startY, colour, id));
                curX = startX; curY = startY;
                idx = 2;
                while (idx < coords.size() - 1) {
                    curX = coords.get(idx); curY = coords.get(idx+1);
                    instructions.add(new PathInstruction('L', curX, curY, colour, id));
                    idx += 2;
                }
                break;
            case 'L': // line
            case 'l':
                while (idx < coords.size() - 1) {
                    curX = coords.get(idx); curY = coords.get(idx+1);
                    instructions.add(new PathInstruction('L', curX, curY, colour, id));
                    idx += 2;
                }
                break;
            case 'C': //curve
            case 'c':
                while (idx < coords.size() - 5) {
                    double cp1x = coords.get(idx),   cp1y = coords.get(idx+1);
                    double cp2x = coords.get(idx+2), cp2y = coords.get(idx+3);
                    double endX = coords.get(idx+4), endY = coords.get(idx+5); 

                    double distance = Math.sqrt(Math.pow(endX - curX, 2) + Math.pow(endY - curY, 2));
                    double estimatedGridDistance = distance * ((double) gridSize / 1000.0);
                    int segments = (int) Math.max(4, Math.ceil(estimatedGridDistance / 2.0));

                    for (int step = 1; step <= segments; step++) {
                        double t = (double) step / segments;
                        double x = Math.pow(1-t, 3)*curX + 3*Math.pow(1-t, 2)*t*cp1x + 3*(1-t)*t*t*cp2x + t*t*t*endX;
                        double y = Math.pow(1-t, 3)*curY + 3*Math.pow(1-t, 2)*t*cp1y + 3*(1-t)*t*t*cp2y + t*t*t*endY;
                        instructions.add(new PathInstruction('L', x, y, colour, id));
                    }
                    curX = endX; curY = endY;
                    idx += 6;
                }
                break;
        }
    }

    private static void addIntersection(List<Double> intersections, PathInstruction a, PathInstruction b, double y) {
        if ((a.y <= y && b.y > y) || (b.y <= y && a.y > y)) {
            if (b.y - a.y != 0) {
                double x = a.x + (y - a.y) * (b.x - a.x) / (b.y - a.y);
                intersections.add(x);
            }
        }
    }
}
