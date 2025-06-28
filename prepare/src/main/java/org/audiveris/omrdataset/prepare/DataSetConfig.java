//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    D a t a S e t C o n f i g                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omrdataset.prepare;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Class <code>DataSetConfig</code> describes how a source dataset should be used
 * to compose a Yolo dataset.
 *
 * @author Hervé Bitteur
 */
public abstract class DataSetConfig
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Location of the source dataset. */
    public String source;

    /**
     * Name of the sub-folder where score images are located.
     * It is generally a folder filled with .png files.
     */
    public String images;

    /**
     * Name of the sub-folder where score annotations are located.
     * It can be a folder filled with specifically formatted files, parallel to the images files,
     * describing the symbols (class name, bounding box) found in the score image.
     */
    public String annotations;

    /** Tasks to be actually performed among 'histogram', 'checking', 'train', 'val'. */
    public List<String> tasks;

    /** (Optional) specification for some visual checking. */
    public Checking checking;

    /**
     * List of the score images names selected for the training part.
     */
    public List<String> train;

    /**
     * List of the score images names selected for the validation part.
     */
    public List<String> val;

    //~ Constructors -------------------------------------------------------------------------------

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" {")//
                .append("\n  source:").append(source)//
                .append("\n  images:").append(images)//
                .append("\n  annotations:").append(annotations);

        if (tasks != null) {
            sb.append("\n  tasks:").append(tasks.stream().collect(Collectors.joining(", ")));
        }

        if (checking != null) {
            sb.append("\n  checking:").append(checking);
        }

        if (train != null) {
            sb.append("\n  train.size:").append(train.size());
        }

        if (val != null) {
            sb.append("\n  val.size:").append(val.size());
        }

        return sb.append("\n}").toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    public static class Checking
    {
        /** Folder where annotated images are written. */
        public String output;

        /** Print the class name (rather than the class ordinal). */
        public boolean draw_name;

        /** Labels not to be drawn. */
        public List<String> hidden_labels;

        /** Should we check the whole train set. */
        public boolean use_train;

        /** Should we check the whole val set. */
        public boolean use_val;

        /** List of the images to check. */
        public List<String> selection;

        @Override
        public String toString ()
        {
            return new StringBuilder("{")//
                    .append("output:").append(output) //
                    .append(" draw_name:").append(draw_name) //
                    .append(" use_train:").append(use_train) //
                    .append(" use_val:").append(use_val) //
                    .append(" selection.size:").append(selection.size()) //
                    .append('}').toString();
        }
    }
}
