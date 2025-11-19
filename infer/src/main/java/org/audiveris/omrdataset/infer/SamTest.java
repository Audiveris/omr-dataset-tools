//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          S a m T e s t                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omrdataset.infer;

import org.audiveris.omrdataset.prepare.YoloLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Class <code>SamTest</code> is meant to test the results of SAM-2 (Segment Anything Model).
 * Models are:
 * dict_keys([
 * 'sam_h.pt', 'sam_l.pt', 'sam_b.pt', 'mobile_sam.pt',
 * 'sam2_t.pt', 'sam2_s.pt', 'sam2_b.pt', 'sam2_l.pt',
 * 'sam2.1_t.pt', 'sam2.1_s.pt', 'sam2.1_b.pt', 'sam2.1_l.pt'])
 *
 * @author Hervé Bitteur
 */
public class SamTest
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SamTest.class);

    private static final YoloLabel[] classes = YoloLabel.values();

    //~ Instance fields ----------------------------------------------------------------------------

    final String imgFolder;

    //~ Constructors -------------------------------------------------------------------------------

    public SamTest (String imgFolder)

    {
        this.imgFolder = imgFolder;
    }
    //~ Methods ------------------------------------------------------------------------------------

    public void process (Path txtPath)
    {
        try {
            System.out.println("SamTest on " + txtPath);

            final String filename = txtPath.getFileName().toString();
            final int dot = filename.indexOf('.');
            final String sansExt = filename.substring(0, dot);
            final Path imgPath = Paths.get(imgFolder).resolve(sansExt + ".png");

            final BufferedImage orgImage = ImageIO.read(imgPath.toFile());
            final int imgWidth = orgImage.getWidth();
            final int imgHeight = orgImage.getHeight();

            final Path outPath = txtPath.getParent().resolve(sansExt + "-seg.png");

            final BufferedImage off_Image = new BufferedImage(
                    imgWidth,
                    imgHeight,
                    BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g2d = off_Image.createGraphics();
            g2d.drawImage(orgImage, null, 0, 0);

            g2d.setColor(Color.RED);

            final List<String> lines = Files.readAllLines(txtPath);
            lines.forEach(l -> {
                //System.out.println(l);
                final String[] words = l.split("\\s+");
                final int cid = Integer.decode(words[0]);
                System.out.println("Class: " + classes[cid]);
                final int nbPts = (words.length - 1) / 2;
                final int[] xPoints = new int[nbPts];
                final int[] yPoints = new int[nbPts];
                for (int i = 1; i < words.length - 1; i += 2) {
                    final double nx = Double.parseDouble(words[i]);
                    final double ny = Double.parseDouble(words[i + 1]);
                    final int x = (int) Math.rint(nx * imgWidth);
                    xPoints[(i - 1) / 2] = x;
                    final int y = (int) Math.rint(ny * imgHeight);
                    yPoints[(i - 1) / 2] = y;
                    //final Point pt = new Point(x, y);
                    //System.out.printf("%10d : %s %n", ++count, pt);

                }

                g2d.drawPolygon(xPoints, yPoints, nbPts);

            });

            ImageIO.write(off_Image, "png", outPath.toFile());
            logger.info("Image printed as {}", outPath);

        } catch (IOException ex) {
            logger.warn("Error on {}", txtPath, ex);
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    public static void main (String... args)
        throws Exception
    {
        final SamTest sam = new SamTest("D:\\soft\\DeepScores\\ds2_dense\\images");

        final Visitor visitor = new Visitor(sam);

        Files.walkFileTree(Paths.get("../data/segmentation/val_auto_annotate_labels"), visitor);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------//
    // Visitor //
    //---------//
    private static class Visitor
            extends SimpleFileVisitor<Path>
    {
        private static final List<String> supported = Arrays.asList(".txt");

        private final SamTest sam;

        public Visitor (SamTest sam)
        {
            this.sam = sam;
        }

        @Override
        public FileVisitResult visitFile (Path path,
                                          BasicFileAttributes attr)
        {
            final int dot = path.toString().lastIndexOf('.');
            final String ext = path.toString().substring(dot);

            if (supported.contains(ext)) {
                System.out.println("\nProcessing " + path);
                try {
                    sam.process(path);
                } catch (Exception ex) {
                    logger.warn("Error processing {} {}", path, ex);
                }
            }

            return CONTINUE;
        }
    }

}
