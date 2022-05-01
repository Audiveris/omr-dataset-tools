//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S o u r c e I n f o                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omrdataset.extraction;

import org.audiveris.omrdataset.api.Context;
import org.audiveris.omrdataset.api.Context.Metadata;
import static org.audiveris.omrdataset.training.App.ARCHIVE_FOLDER_PATTERN;

import org.nd4j.linalg.api.ndarray.INDArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

/**
 * Class {@code SourceInfo} describes the source of data set.
 *
 * @author Hervé Bitteur
 */
public abstract class SourceInfo
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SourceInfo.class);

    /** Known collections. To be augmented as needed. */
    public static final Map<Integer, Collection> collections = new TreeMap<>();

    static {
        collections.put(0, new Collection(0, "D", XmlFormat.ZHAW, "DeepScores"));
        collections.put(1, new Collection(1, "M", XmlFormat.MUSESCORE, "MuseScore"));
        ///collections.put(2, new Collection(2, "O", XmlFormat.MUSESCORE, "OtherUser"));
    }

    /** Collection to archives paths. */
    private static Map<UCollectionId, Map<UArchiveId, Path>> toArchivePath = new TreeMap<>();

    /** Collection to archives sheetIndex instances. */
    private static Map<UCollectionId, Map<UArchiveId, SheetIndex>> toArchiveIndex = new TreeMap<>();

    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    private SourceInfo ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getPath //
    //---------//
    public static Path getPath (UArchiveId uArchiveId)
    {
        final UCollectionId uCollectionId = uArchiveId.getCollectionId();
        final Map<UArchiveId, Path> a2p = toArchivePath.get(uCollectionId);

        if (a2p == null) {
            logger.warn("No collection {}", uCollectionId);

            return null;
        }

        final Path path = a2p.get(uArchiveId);

        if (path == null) {
            logger.warn("No archive {} in collection {}", uArchiveId, uCollectionId);
        }

        return path;
    }

    //---------//
    // getPath //
    //---------//
    public static Path getPath (USheetId uSheetId)
    {
        final Map<UArchiveId, SheetIndex> a2i = toArchiveIndex.get(uSheetId.getCollectionId());

        if (a2i == null) {
            logger.warn("No collection {}", uSheetId.collectionId);

            return null;
        }

        final SheetIndex si = a2i.get(uSheetId.getArchiveId());

        if (si == null) {
            logger.warn("No sheetIndex for archive {}", uSheetId.archiveId);

            return null;
        }

        final Path path = si.getPath(uSheetId.sheetId);

        if (path == null) {
            logger.warn("No path for sheet {}", uSheetId.sheetId);
        }

        return path;
    }

    //------------//
    // getSheetId //
    //------------//
    public static USheetId getSheetId (UArchiveId uArchiveId,
                                       Path sheetPath)
    {
        final SheetIndex si = getSheetIndex(uArchiveId);
        final Integer sheetId = si.getId(sheetPath);

        return new USheetId(uArchiveId, sheetId);
    }

    //---------------//
    // getSheetIndex //
    //---------------//
    public static SheetIndex getSheetIndex (UArchiveId uArchiveId)
    {
        final UCollectionId uCollectionId = uArchiveId.getCollectionId();
        final Map<UArchiveId, SheetIndex> a2i = toArchiveIndex.get(uCollectionId);

        if (a2i == null) {
            logger.warn("No collection {}", uCollectionId);

            return null;
        }

        SheetIndex si = a2i.get(uArchiveId);

        if (si == null) {
            a2i.put(uArchiveId, si = new SheetIndex());
        }

        return si;
    }

    //---------------//
    // parseSymbolId //
    //---------------//
    public static <S extends Enum, C extends Context<S>> USymbolId parseSymbolId (String[] cols,
                                                                                  C context)
    {
        final int collectionId = Integer.parseInt(cols[context.getCsv(Metadata.COLLECTION)]);
        final int archiveId = Integer.parseInt(cols[context.getCsv(Metadata.ARCHIVE)]);
        final int sheetId = Integer.parseInt(cols[context.getCsv(Metadata.SHEET_ID)]);
        final int symbolId = Integer.parseInt(cols[context.getCsv(Metadata.SYMBOL_ID)]);

        return new USymbolId(collectionId, archiveId, sheetId, symbolId);
    }

    //---------------//
    // parseSymbolId //
    //---------------//
    /**
     * Parse an USymbolId value out of the provided metadata read from a .CSV file.
     * <p>
     * Metadata are all the cells located right after the label cell: [true shape, collection, ...]
     *
     * @param <S>
     * @param <C>
     * @param meta    the vector of metadata [true shape, collection ...]
     * @param context
     * @return the parsed USymbolId value
     */
    public static <S extends Enum, C extends Context<S>> USymbolId parseSymbolId (INDArray meta,
                                                                                  C context)
    {
        final int collectionId = (int) Math.rint(meta.getDouble(Metadata.COLLECTION.ordinal()));
        final int archiveId = (int) Math.rint(meta.getDouble(Metadata.ARCHIVE.ordinal()));
        final int sheetId = (int) Math.rint(meta.getDouble(Metadata.SHEET_ID.ordinal()));
        final int symbolId = (int) Math.rint(meta.getDouble(Metadata.SYMBOL_ID.ordinal()));

        return new USymbolId(collectionId, archiveId, sheetId, symbolId);
    }

    //-----------------//
    // lookupArchiveId //
    //-----------------//
    /**
     * Walk up file hierarchy to a folder which matches an archive name.
     *
     * @param input some file within an archive
     * @return the ID of the containing archive or null
     */
    public static UArchiveId lookupArchiveId (Path input)
    {
        Path parent = input;

        while (parent != null) {
            final String fn = parent.getFileName().toString();
            final Matcher m = ARCHIVE_FOLDER_PATTERN.matcher(fn);

            if (m.matches()) {
                final String num = m.group("num");
                final int archiveId = Integer.parseInt(num);

                // Check parent as collection name
                final Path archiveParent = parent.getParent();
                final UCollectionId uCollectionId = getCollectionId(archiveParent);

                if (uCollectionId == null) {
                    throw new IllegalStateException("Unknown collection: " + archiveParent);
                }

                // Make sure this archive is registered with its sheetIndex
                Map<UArchiveId, Path> a2p = toArchivePath.get(uCollectionId);
                if (a2p == null) {
                    toArchivePath.put(uCollectionId, a2p = new HashMap<>());
                }

                UArchiveId uArchiveId = new UArchiveId(archiveId, uCollectionId);
                Path path = a2p.get(uArchiveId);
                if (path == null) {
                    a2p.put(uArchiveId, parent);

                    SheetIndex si = SheetIndex.getSheetIndex(parent);
                    Map<UArchiveId, SheetIndex> a2s = new HashMap<>();
                    toArchiveIndex.put(uCollectionId, a2s);
                    a2s.put(uArchiveId, si);
                } else if (!path.equals(parent)) {
                    throw new IllegalStateException(
                            "Conflict on archive ID:" + archiveId + " " + parent + " " + path);
                }

                return uArchiveId;

            }

            parent = parent.getParent();
        }

        return null;
    }

    //-----------------//
    // getCollectionId //
    //-----------------//
    public static UCollectionId getCollectionId (Path path)
    {
        final String name = path.getFileName().toString().toLowerCase();

        for (Collection collection : collections.values()) {
            if (collection.folderName.toLowerCase().contains(name)) {
                return new UCollectionId(collection.id);
            }
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // XmlFormat //
    //-----------//
    /**
     * The XML format used in source annotations XML files.
     */
    public static enum XmlFormat
    {
        /** Uses ZhawAnnotations class. */
        ZHAW,
        /** Uses SheetAnnotations class. */
        MUSESCORE;
    }

    //------------//
    // Collection //
    //------------//
    /**
     * The source collection.
     * <p>
     * Different collections must have different names.
     * The collection name is the file name of the parent folder of the archives folders.
     * <p>
     * Moreover, for convenience in print outs, every collection should have a unique initial.
     */
    public static class Collection
    {

        /** Unique ID in collections. */
        public final int id;

        /** Preferred initial. */
        public final String initial;

        /** Folder name. */
        public final String folderName;

        /** The precise format to unmarshall sheet annotations. */
        private XmlFormat xmlFormat;

        private Collection (int id,
                            String initial,
                            XmlFormat xmlFormat,
                            String folderName)
        {
            this.id = id;
            this.initial = initial;
            this.xmlFormat = xmlFormat;
            this.folderName = folderName;
        }

        /**
         * @return the xmlFormat
         */
        public XmlFormat getXmlFormat ()
        {
            return xmlFormat;
        }

        /**
         * @param xmlFormat the xmlFormat to set
         */
        public void setXmlFormat (XmlFormat xmlFormat)
        {
            this.xmlFormat = xmlFormat;
        }
    }

    //---------------//
    // UCollectionId //
    //---------------//
    /** Universal Collection ID. */
    public static class UCollectionId
            implements Comparable<UCollectionId>
    {

        public final int collectionId;

        public UCollectionId (int collectionId)
        {
            this.collectionId = collectionId;
        }

        public Collection getCollection ()
        {
            return collections.get(collectionId);
        }

        @Override
        public int compareTo (UCollectionId other)
        {
            return Integer.compare(collectionId, other.collectionId);
        }

        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof UCollectionId)) {
                return false;
            }

            final UCollectionId other = (UCollectionId) obj;

            return collectionId == other.collectionId;
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            hash = 37 * hash + this.collectionId;
            return hash;
        }

        @Override
        public String toString ()
        {
            return collections.get(collectionId).folderName;
        }
    }

    //------------//
    // UArchiveId //
    //------------//
    /** Universal Archive ID. */
    public static class UArchiveId
    {

        public final int archiveId;

        private final int collectionId;

        public UArchiveId (int archiveId,
                           int collectionId)
        {
            this.archiveId = archiveId;
            this.collectionId = collectionId;
        }

        public UArchiveId (
                int archiveId,
                UCollectionId uCollectionId)
        {
            this(archiveId, uCollectionId.collectionId);
        }

        public UCollectionId getCollectionId ()
        {
            return new UCollectionId(collectionId);
        }

        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof UArchiveId)) {
                return false;
            }

            final UArchiveId other = (UArchiveId) obj;

            return (archiveId == other.archiveId)
                           && (collectionId == other.collectionId);
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            hash = 41 * hash + this.archiveId;
            hash = 41 * hash + this.collectionId;
            return hash;
        }

        @Override
        public String toString ()
        {
            return collections.get(collectionId).folderName + "-" + archiveId;
        }
    }

    //----------//
    // USheetId //
    //----------//
    /** Universal Sheet ID. */
    public static class USheetId
    {

        public final int sheetId;

        public final int archiveId;

        private final int collectionId;

        public USheetId (int collectionId,
                         int archiveId,
                         int sheetId)
        {
            this.sheetId = sheetId;
            this.archiveId = archiveId;
            this.collectionId = collectionId;
        }

        public USheetId (UArchiveId uArchiveId,
                         int sheetId)
        {
            this(uArchiveId.collectionId, uArchiveId.archiveId, sheetId);
        }

        public UCollectionId getCollectionId ()
        {
            return new UCollectionId(collectionId);
        }

        public UArchiveId getArchiveId ()
        {
            return new UArchiveId(archiveId, collectionId);
        }

        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof USheetId)) {
                return false;
            }

            final USheetId other = (USheetId) obj;

            return (sheetId == other.sheetId)
                           && (archiveId == other.archiveId)
                           && (collectionId == other.collectionId);
        }

        @Override
        public int hashCode ()
        {
            int hash = 3;
            hash = 79 * hash + this.sheetId;
            hash = 79 * hash + this.archiveId;
            hash = 79 * hash + this.collectionId;
            return hash;
        }

        @Override
        public String toString ()
        {
            return collections.get(collectionId).initial + "-" + archiveId + "-" + sheetId;
        }
    }

    //-----------//
    // USymbolId //
    //-----------//
    /** Universal Symbol ID. */
    public static class USymbolId
    {

        public final int symbolId;

        public final int sheetId;

        public final int archiveId;

        public final int collectionId;

        public USymbolId (int collectionId,
                          int archiveId,
                          int sheetId,
                          int symbolId)
        {
            this.collectionId = collectionId;
            this.archiveId = archiveId;
            this.sheetId = sheetId;
            this.symbolId = symbolId;
        }

        public USymbolId (USheetId uSheetId,
                          int symbolId)
        {
            this(uSheetId.collectionId, uSheetId.archiveId, uSheetId.sheetId, symbolId);
        }

        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof USymbolId)) {
                return false;
            }

            final USymbolId other = (USymbolId) obj;

            return (symbolId == other.symbolId)
                           && (sheetId == other.sheetId)
                           && (archiveId == other.archiveId)
                           && (collectionId == other.collectionId);
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            hash = 23 * hash + this.symbolId;
            hash = 23 * hash + this.sheetId;
            hash = 23 * hash + this.archiveId;
            hash = 23 * hash + this.collectionId;
            return hash;
        }

        public String toShortString ()
        {
            return sheetId + "-" + symbolId;
        }

        @Override
        public String toString ()
        {
            return collections.get(collectionId).initial + "-" + archiveId + "-" + sheetId + "-"
                           + symbolId;
        }
    }
}
