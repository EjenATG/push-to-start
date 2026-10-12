public class ThirdDimension
{
    public static void main(String[] args) throws InterruptedException {
        // init
        double[] fbl = {-1, -1, -1};
        double[] fbr = { 1, -1, -1};
        double[] ftr = { 1,  1, -1};
        double[] ftl = {-1,  1, -1};

        double[] bbl = {-1, -1,  1};
        double[] bbr = { 1, -1,  1};
        double[] btr = { 1,  1,  1};
        double[] btl = {-1,  1,  1};
        
        double rx = 0;
        double ry = 0;
        double rz = 0;

        double scale = 40;

        int width = 60;
        int height = 60;

        int centreX = width / 2;
        int centreY = height / 2;
        
        while (true) {
            boolean[][] plane = new boolean[height][width];

            double[] rot = {rx, ry, rz};

            double[] pfbl = rotation(fbl, rot);
            double[] pfbr = rotation(fbr, rot);
            double[] pftr = rotation(ftr, rot);
            double[] pftl = rotation(ftl, rot);

            double[] pbbl = rotation(bbl, rot);
            double[] pbbr = rotation(bbr, rot);
            double[] pbtr = rotation(btr, rot);
            double[] pbtl = rotation(btl, rot);

            int[][] vertices = {
                project(pfbl, scale, centreX, centreY),
                project(pfbr, scale, centreX, centreY),
                project(pftr, scale, centreX, centreY),
                project(pftl, scale, centreX, centreY),

                project(pbbl, scale, centreX, centreY),
                project(pbbr, scale, centreX, centreY),
                project(pbtr, scale, centreX, centreY),
                project(pbtl, scale, centreX, centreY)
            };

            // connect
            connect(plane, vertices[0], vertices[1]);
            connect(plane, vertices[1], vertices[2]);
            connect(plane, vertices[2], vertices[3]);
            connect(plane, vertices[3], vertices[0]);

            connect(plane, vertices[4], vertices[5]);
            connect(plane, vertices[5], vertices[6]);
            connect(plane, vertices[6], vertices[7]);
            connect(plane, vertices[7], vertices[4]);

            connect(plane, vertices[0], vertices[4]);
            connect(plane, vertices[1], vertices[5]);
            connect(plane, vertices[2], vertices[6]);
            connect(plane, vertices[3], vertices[7]);
            
            // project
            System.out.print("\033[H");
            for (int y = 0; y < plane.length; y++) {
                for (int x = 0; x < plane[0].length; x++) {
                    if (plane[y][x]) System.out.print("|||");
                    else System.out.print("   ");
                }
                System.out.println();
            }
            
            // anim
            ry += 0.1;
            
            Thread.sleep(100);
        }
    }

    static double[] rotation(double[] point, double[] angles) {

        double x = point[0];
        double y = point[1];
        double z = point[2];

        // x
        double cosX = Math.cos(angles[0]);
        double sinX = Math.sin(angles[0]);
        double y1 = y * cosX - z * sinX;
        double z1 = y * sinX + z * cosX;
        double x1 = x;

        // y
        double cosY = Math.cos(angles[1]);
        double sinY = Math.sin(angles[1]);
        double x2 = x1 * cosY - z1 * sinY;
        double z2 = x1 * sinY + z1 * cosY;
        double y2 = y1;

        // z
        double cosZ = Math.cos(angles[2]);
        double sinZ = Math.sin(angles[2]);

        double x3 = x2 * cosZ - y2 * sinZ;
        double y3 = x2 * sinZ + y2 * cosZ;
        double z3 = z2;

        return new double[] {x3, y3, z3};
    }

    static int[] project(double[] point, double scale, int centreX,int centreY) {

        double x = point[0];
        double y = point[1];
        double z = point[2];

        double cameraDistance = 3;
        double depth = z + cameraDistance;

        int newX = (int)Math.round(x * scale / depth + centreX);
        int newY = (int)Math.round(y * scale / depth + centreY);
        return new int[] {newX, newY};
    }

    static void connect(boolean[][] plane, int[] a,int[] b) {

        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        int steps = (int)Math.max(Math.abs(dx), Math.abs(dy));

        if (steps == 0) {
            if (a[1] >= 0 && a[1] < plane.length &&
                a[0] >= 0 && a[0] < plane[0].length) {
                plane[a[1]][a[0]] = true;
            }
            return;
        }

        for (int i = 0; i <= steps; i++) {
            double progress = (double)i / steps;
            int x = (int)Math.round(a[0] + dx * progress);
            int y =(int)Math.round(a[1] + dy * progress);

            if (x >= 0 && x < plane[0].length && y >= 0 && y < plane.length) {
                plane[y][x] = true;
            }
        }
    }
}