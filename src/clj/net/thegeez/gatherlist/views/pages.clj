(ns net.thegeez.gatherlist.views.pages
  (:require [net.cgrand.enlive-html :as html]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce])
  (:import [org.joda.time Period]
           [org.joda.time.format PeriodFormatter PeriodFormatterBuilder]))

;; h/t http://stackoverflow.com/a/2796052

(def ^org.joda.time.format.PeriodFormatter period-formatter
  (-> (org.joda.time.format.PeriodFormatterBuilder.)
      .appendYears
      (.appendSuffix " years, ")
      .appendMonths
      (.appendSuffix " months, ")
      .appendWeeks
      (.appendSuffix " weeks, ")
      .appendDays
      (.appendSuffix " days, ")
      .appendHours
      (.appendSuffix " hours, ")
      .appendMinutes
      (.appendSuffix " minutes, ")
      .appendSeconds
      (.appendSuffix " seconds")
      .printZeroNever
      .toFormatter))

(defn time-ago-str [from to]
  (let [from (time-coerce/from-long from)
        period (org.joda.time.Period. from to)
        period-str (.print period-formatter period)
        period-str (if (seq period-str)
                     (str period-str " ago")
                     (str "moments ago"))]
    [:span {:title from}
     period-str]))

(defmulti to-html :type)

(defmethod to-html :title [item]
  (let [{:keys [to from by time]} item
        now (time/now)]
    [:div.panel.panel-default
     [:div.panel-heading
      [:h3.panel-title "Title changed: " to (when from (str " (was: " from ")")) " by " by " " (time-ago-str time now)]]]))

(defmethod to-html :item [item]
  (let [{:keys [text by time]} item
        now (time/now)]
    [:div.panel.panel-default
     [:div.panel-heading
      [:h3.panel-title by " " (time-ago-str time now)]]
     [:div.panel-body
      text]]))

(defmethod to-html :default [item]
  [:div "PR_STR" (pr-str item)])

(defn page [page]
  (html/html
   [:div
    [:h1 (:title page)]
    [:div
     (map to-html (:stream (:stream page)))]
    [:div#updates
     {:class "alert alert-info hidden"}]]))
