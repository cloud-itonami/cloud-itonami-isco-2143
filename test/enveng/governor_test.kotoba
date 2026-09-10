(ns enveng.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [enveng.store :as store]
            [enveng.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-project! st {:project-id "site-1" :site-name "Industrial Cleanup A" :client-id "client-1"})
    st))

(deftest ok-on-clean-remediation-plan
  (let [st (fresh-store)
        proposal {:op :draft-remediation-plan :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:project-id "site-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-project
  (let [st (fresh-store)
        proposal {:op :draft-remediation-plan :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:project-id "no-such-site"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-project (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :draft-remediation-plan :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:project-id "site-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-attempt-to-issue-certified-design
  (let [st (fresh-store)
        proposal {:op :issue-certified-design :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:project-id "site-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-certification (:rule %)) (:violations v)))))

(deftest hard-on-attempt-to-certify-compliance
  (let [st (fresh-store)
        proposal {:op :certify-compliance :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:project-id "site-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-certification (:rule %)) (:violations v)))))

(deftest escalates-on-regulatory-risk-flag
  (let [st (fresh-store)
        proposal {:op :flag-regulatory-risk :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:project-id "site-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-hazmat-handling
  (let [st (fresh-store)
        proposal {:op :draft-remediation-plan :effect :propose :confidence 0.9 :stake :medium
                  :scope :remediation :tags #{:hazardous :petroleum}}
        v (governor/check {:project-id "site-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :log-site-data :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:project-id "site-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:project-id "site-1" :op :log-site-data})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "site-1"))))
    (is (= 1 (count (store/ledger st))))))
