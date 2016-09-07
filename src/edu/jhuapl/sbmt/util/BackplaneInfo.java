package edu.jhuapl.sbmt.util;


import java.util.ArrayList;
import java.util.List;

/**
 * Enumeration containing the values and comments to use for FITS tags describing plane data stored in
 * FITS data cubes. The enumeration name references the type of data stored in a given plane. This
 * way the user can choose their own value for the FITS keyword (i.e. "PLANE1" or "PLANE10").
 *
 * @author espirrc1
 *
 * nguyel1:
 * This was modified by espirrc1 from ALTWG's PlaneInfo.java for the EROS MSI backplanes PDS delivery,
 * although the backplanes here are not specific to the MSI instrument. The name of the class has been
 * changed to avoid confusion with ALTWG's PlaneInfo enumeration, which is specific to the OLA
 * instrument on OSIRIS-REx. This file contains generic backplanes supported by SBMT.
 */
public enum BackplaneInfo
{
  PIXEL("Pixel value", "",""),
  X("X coordinate of vertices", "[km]", "km"),
  Y("Y coordinate of vertices", "[km]", "km"),
  Z("Z coordinate of vertices", "[km]", "km"),
  LAT("Latitude of vertices", "[deg]", "deg"),
  LON("Longitude of vertices", "[deg]", "deg"),
  DIST("Radius of vertices", "[km]", "km"),
  INC("Incidence angle", "[deg]", ""),
  EMI("Emission angle", "[deg]", ""),
  PHASE("Phase angle", "[deg]", ""),
  HSCALE("Horizontal pixel scale", "[km]", ""),
  VSCALE("Vertical pixel scale", "[km]", ""),
  SLOPE("Slope", "[deg]",""),
  EL("Elevation", "[m]", ""),
  GRAVACC("Gravitational acceleration", "[m/s^2]", ""),
  GRAVPOT("Gravitational potential", "[J/kg]", "");

  private String keyValue; // value associated with FITS keyword
  private String comment; // comment associated with FITS keyword
  private String units; // units associated with the plane. Usually in PDS4 nomenclature

  /**
   * Where the general fits header syntax is:
   * 'KEYWORD = VALUE // COMMENT'
   *
   * @param keyVal - VALUE
   * @param comment - COMMENT
   * @param units - used when needing to assign units along w/ actual values, usually in a printout to screen
   *  or to an ascii file.
   */
  BackplaneInfo(String keyVal, String comment, String units) {
    this.keyValue = keyVal;
    this.comment = comment;
    this.units = units;
  }

  public String value() {
    return keyValue;
  }

  public String comment() {
    return comment;
  }

  public String units() {
    return units;
  }

  /**
   * Return a list of planes. The list is solely based on the number of planes passed, and the order
   * is based solely on the order in which planes are listed in the enumeration set.
   * @param numPlanes
   * @return
   */
  public static List<BackplaneInfo> getPlanes(int numPlanes) {
    List<BackplaneInfo> planeList = new ArrayList<BackplaneInfo>();

    int maxPlanes = BackplaneInfo.values().length;
    if (numPlanes > maxPlanes) {
      System.out.println(
          "ERROR! Number of planes requested > maximum number of planes in PlaneInfo! Stopping with error!");
      System.exit(1);
    }
    int ii = 0;
    for (BackplaneInfo thisPlane : values()) {
      planeList.add(thisPlane);
      ii = ii + 1;
      if (ii >= numPlanes) {
        break;
      }
    }

    return planeList;
  }


}
