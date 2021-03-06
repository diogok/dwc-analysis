(ns dwc-analysis.geo
  (:use [clojure.data.json :only [read-str write-str]])
  (:require [cljts.transform :as transform])
  (:require [cljts.analysis :as analysis])
  (:require [cljts.relation :as relation])
  (:require [cljts.geom :as geom])
  (:require [cljts.io :as io])
  (:import [com.vividsolutions.jts.geom
            GeometryFactory
            PrecisionModel
            PrecisionModel$Type
            Coordinate
            LinearRing
            Point
            Polygon
            Geometry]))

(def point geom/point)
(def c geom/c)
(def area geom/area)
(def utm transform/utmzone)
(def intersects? relation/intersects?)
(def distance analysis/distance)

(defn round2
  "Round a double to the given precision (number of significant digits)"
  [precision d]
  (let [factor (Math/pow 10 precision)]
        (/ (Math/round (* d factor)) factor)))

(defn point?
  [occ] 
   (and 
     (not (nil? occ))
     (not (empty? occ))
     (not (nil? (:decimalLatitude occ)))
     (not (nil? (:decimalLongitude occ)))
     (number? (:decimalLatitude occ))
     (number? (:decimalLongitude occ))
     (not (= 0 (:decimalLatitude occ)))
     (not (= 0 (:decimalLongitude occ)))
     (not (= 0.0 (:decimalLatitude occ)))
     (not (= 0.0 (:decimalLongitude occ)))
     (<= (:decimalLongitude occ) 180)
     (<= (:decimalLongitude occ) 180)
     (<= (:decimalLatitude occ) 90)
     (>= (:decimalLongitude occ) -180)
     (>= (:decimalLatitude occ) -90)))

(defn to-point
  [p] 
  (point (c (:decimalLongitude p) (:decimalLatitude p))))

(defn to-utm
  ([p] (to-utm p "EPSG:4326"))
  ([p crs]
    (transform/reproject p crs (utm p))))

(defn distance-in-meters
  ([p0 p1] (distance-in-meters p0 p1 "EPSG:4326"))
  ([p0 p1 crs]
    (analysis/distance
      (to-utm p0 crs )
      (to-utm p1 crs ))))

(defn area-in-meters
  ""
  ([polygon] (area-in-meters polygon "EPSG:4326"))
  ([polygon crs] 
   (area
     (transform/reproject
       polygon crs (utm polygon)))))

(defn convex-hull
  ""
  [points] 
   (analysis/convex-hull 
     (reduce analysis/union
       (pmap
         (fn [points] (reduce analysis/union points))
         (partition-all 1000 points)))))

(defn get-points
  ""
  [feature]
  (map point (.getCoordinates feature)))

(defn buffer-in-meters
  ""
  ([point meters] (buffer-in-meters point meters "EPSG:4326"))
  ([point meters crs]
    (transform/reproject 
      (analysis/buffer
        (transform/reproject point crs (utm point))
        meters) (utm point) crs)))

(defn union
  ""
  [ features ]
  (reduce analysis/union
    (pmap 
      (fn [features] (reduce analysis/union features))
      (partition-all 1000 features))))

(defn as-geojson
  ""
  [feature]
  (if (nil? feature) nil
    (read-str (io/write-geojson feature) :key-fn keyword)))

