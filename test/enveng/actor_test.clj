(ns enveng.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [enveng.actor :as actor]
            [enveng.store :as store]
            [enveng.advisor :as advisor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-project! st {:project-id "site-1" :site-name "Industrial Cleanup A" :client-id "client-1"})
    st))

(deftest clean-flow-ok-proposal-commits
  (let [st (fresh-store)
        g (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        req {:project-id "site-1" :op :draft-remediation-plan :stake :low}
        result (actor/run-request! g req {} "thread-1")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/records-of st "site-1"))))))

(deftest hard-violation-results-in-hold
  (let [st (fresh-store)
        g (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        req {:project-id "no-such-site" :op :draft-remediation-plan :stake :low}
        result (actor/run-request! g req {} "thread-2")]
    (is (= :done (:status result)))
    (is (zero? (count (store/records-of st "site-1"))))))

(deftest escalation-results-in-interrupt
  (let [st (fresh-store)
        g (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        req {:project-id "site-1" :op :flag-regulatory-risk :stake :high}
        result (actor/run-request! g req {} "thread-3")]
    (is (= :interrupted (:status result)))
    (is (zero? (count (store/records-of st "site-1"))))))

(deftest escalated-approval-resumes-and-commits
  (let [st (fresh-store)
        g (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        req {:project-id "site-1" :op :flag-regulatory-risk :stake :high}
        run1 (actor/run-request! g req {} "thread-4")
        _ (is (= :interrupted (:status run1)))
        run2 (actor/approve! g "thread-4")]
    (is (= :done (:status run2)))
    (is (= 1 (count (store/records-of st "site-1"))))))

(deftest ledger-records-all-dispositions
  (let [st (fresh-store)
        g (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        req-ok {:project-id "site-1" :op :log-site-data :stake :low}
        req-hard {:project-id "no-such-site" :op :draft-remediation-plan :stake :low}
        _ (actor/run-request! g req-ok {} "thread-5")
        _ (actor/run-request! g req-hard {} "thread-6")]
    (is (>= (count (store/ledger st)) 2))))
