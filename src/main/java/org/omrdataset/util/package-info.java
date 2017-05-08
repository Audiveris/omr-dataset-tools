/**
 * Utility package for omrdataset.
 * <p>
 * All 'double' values are marshalled with a <b>maximum</b> number of 3 decimals.
 */
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(value = Jaxb.Double3Adapter.class, type = double.class)
})
package org.omrdataset.util;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
