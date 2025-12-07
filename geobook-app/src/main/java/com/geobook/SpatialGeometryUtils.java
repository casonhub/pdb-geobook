package com.geobook;


 // Utility class for working with Oracle SDO_GEOMETRY objects
 // Provides helper methods for creating and manipulating spatial geometries

public class SpatialGeometryUtils {

    
     // Create a POINT geometry from latitude and longitude
    
    public static String createPoint(double longitude, double latitude) {
        return String.format("SDO_GEOMETRY(2001, 8307, SDO_POINT_TYPE(%.6f, %.6f, NULL), NULL, NULL)", 
                           longitude, latitude);
    }

    
     // Create a simple POLYGON geometry from bounding box coordinates
     
    public static String createRectanglePolygon(double minX, double minY, double maxX, double maxY) {
        return String.format("SDO_GEOMETRY(2003, 8307, NULL, SDO_ELEM_INFO_ARRAY(1,1003,3), " +
                           "SDO_ORDINATE_ARRAY(%.6f, %.6f, %.6f, %.6f))", 
                           minX, minY, maxX, maxY);
    }

    
     // Create a LINESTRING geometry from start and end points
     
    public static String createLineString(double startX, double startY, double endX, double endY) {
        return String.format("SDO_GEOMETRY(2002, 8307, NULL, SDO_ELEM_INFO_ARRAY(1,2,1), " +
                           "SDO_ORDINATE_ARRAY(%.6f, %.6f, %.6f, %.6f))", 
                           startX, startY, endX, endY);
    }

    
     // Create a CIRCLE geometry (polygon with arc)
     
    public static String createCircle(double centerX, double centerY, double radius) {
        double rightX = centerX + radius;
        double topY = centerY + radius;
        return String.format("SDO_GEOMETRY(2003, 8307, NULL, SDO_ELEM_INFO_ARRAY(1,1003,4), " +
                           "SDO_ORDINATE_ARRAY(%.6f, %.6f, %.6f, %.6f, %.6f, %.6f))", 
                           centerX, centerY, rightX, centerY + radius/2, centerX - radius, centerY + radius/2);
    }

    
     // Create a complex POLYGON from array of coordinates
     
    public static String createPolygon(double[] coordinates) {
        if (coordinates.length < 6 || coordinates.length % 2 != 0) {
            throw new IllegalArgumentException("Polygon requires at least 3 coordinate pairs (6 values)");
        }
        
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < coordinates.length; i++) {
            if (i > 0) coords.append(", ");
            coords.append(String.format("%.6f", coordinates[i]));
        }
        
        return String.format("SDO_GEOMETRY(2003, 8307, NULL, SDO_ELEM_INFO_ARRAY(1,1003,1), " +
                           "SDO_ORDINATE_ARRAY(%s))", coords.toString());
    }

    
     // Validate if a geometry string is properly formatted
     
    public static boolean isValidGeometry(String geometryString) {
        if (geometryString == null || geometryString.trim().isEmpty()) {
            return false;
        }
        return geometryString.trim().toUpperCase().startsWith("SDO_GEOMETRY(");
    }

    
     // Extract geometry type from SDO_GEOMETRY string
     
    public static String extractGeometryType(String geometryString) {
        if (!isValidGeometry(geometryString)) {
            return "UNKNOWN";
        }
        
        // Simple pattern matching for geometry type codes
        if (geometryString.contains("2001")) return "POINT";
        if (geometryString.contains("2002")) return "LINESTRING"; 
        if (geometryString.contains("2003")) return "POLYGON";
        
        return "UNKNOWN";
    }

    
     // Generate sample geometry 
    public static String generateSampleGeometry(SpatialEntity.SpatialEntityType type, 
                                               double baseLat, double baseLng) {
        double offset = 0.01; // Small offset for variety
        
        switch (type) {
            case POINT:
                return createPoint(baseLng, baseLat);
                
            case LINESTRING:
                return createLineString(baseLng, baseLat, baseLng + offset, baseLat + offset);
                
            case POLYGON:
                return createPolygon(new double[]{
                    baseLng - offset, baseLat - offset,
                    baseLng + offset, baseLat - offset,
                    baseLng + offset, baseLat + offset,
                    baseLng - offset, baseLat + offset,
                    baseLng - offset, baseLat - offset
                });
                
            case CIRCLE:
                return createCircle(baseLng, baseLat, offset);
                
            case RECTANGLE:
                return createRectanglePolygon(baseLng - offset/2, baseLat - offset/2, 
                                            baseLng + offset/2, baseLat + offset/2);
                
            default:
                return createPoint(baseLng, baseLat);
        }
    }
}
