(ns app
  (:require ["inquirer$default" :as inquirer]
            ["mongodb$default" :as mongodb]
            ["moment$default" :as moment]
            ["console$log" :as log]
            [promesa.core :as p]))

;; https://github.com/babashka/nbb
;; https://github.com/SBoudrias/Inquirer.js/
;; https://www.mongodb.com/languages/mongodb-with-nodejs
;; https://momentjs.com/

(def db "birthday_db")
(def uri (str "mongodb+srv://onthecodeagain:" password "@cluster0.2pbdq.mongodb.net/" db "?retryWrites=true&w=majority"))


(defn store-birthday [name day month]
  (p/let [mongo-client (.connect mongodb/MongoClient uri)
          db (.db mongo-client "birthday_db")
          collection (.collection db "birthdays")]
    (.insertOne collection #js {:name name
                                :day day
                                :month month})
    (.close mongo-client)))


(defn retrive-birthdays [day month]
  (p/let [mongo-client (.connect mongodb/MongoClient uri)
          db (.db mongo-client "birthday_db")
          collection (.collection db "birthdays")
          _birthdays (.toArray (.find collection (clj->js {:day (str day) :month month})))
          birthdays (js->clj _birthdays :keywordize-keys true)]
    (.close mongo-client)
    birthdays))


(def questions [{:name "name"
                 :type "input"
                 :message "Who's birthday is it"}
                {:name "day"
                 :type "input"
                 :message "What day is it (1 to 31)"
                 :validate (fn [v]
                             (<= 1 v 31))}
                {:name "month"
                 :type "list"
                 :message "What day is it"
                 :choices (moment/months)}])



(defn create-entry []
  (p/let [answers (js->clj (inquirer/prompt (clj->js questions)))
          name (.-name answers)
          day (.-day answers)
          month (.-month answers)]
    (log "storing:" name day month)
    (store-birthday name day month)
    (log "birthday stored")))



(defn notify-bdays []
  (let [day (.date (moment))
        month (.format (moment) "MMMM")]
    (p/let [birthdays (retrive-birthdays day month)]
      (if (empty? birthdays)
        (println "No birthdays today 😢")
        (doall
         (println "Birthdays Today! 🎉" day "/" month)
         (doall (map (fn [b]
                       (println (:name b))) birthdays)))))))


(if *command-line-args*
  (cond
    (.includes (.-C *command-line-args*) "notify") (notify-bdays))
  (create-entry))



