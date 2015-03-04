 (ns ewen.boot.boot-checkouts
   (:require [clojure.java.io :as io]
     [boot.core :refer [deftask set-env! with-pre-wrap]]
     [boot.pod :as pod]))



 (defn get-boot-project-env [project-pod project-dir-path]
       (let [boot-file-path (-> (io/file project-dir-path "build.boot")
                                .getAbsolutePath)]
            (pod/with-eval-in project-pod
                              (require '[boot.core]
                                       '[boot.file :as file]
                                       '[clojure.java.io :as io]
                                       '[boot.tmpregistry :as tmp]
                                       '[boot.pod :as pod])
                              (reset! pod/pod-id 1)
                              (->> (io/file ~project-dir-path)
                                   (.getCanonicalFile)
                                   file/split-path
                                   rest
                                   (apply io/file (boot.App/getBootDir) "tmp")
                                   tmp/registry
                                   tmp/init!
                                   (reset! @#'boot.core/tmpregistry))
                              (doto @#'boot.core/boot-env
                                    (reset! {:watcher-debounce 10
                                             :dependencies []
                                             :directories #{}
                                             :source-paths #{}
                                             :resource-paths #{}
                                             :asset-paths #{}
                                             :target-path "target"
                                             :repositories [["clojars" "http://clojars.org/repo/"]
                                                            ["maven-central" "http://repo1.maven.org/maven2/"]]})
                                    (add-watch :boot.core/boot #(@#'boot.core/configure!* %3 %4)))
                              (@#'boot.core/set-fake-class-path!)
                              (@#'boot.core/temp-dir** nil :asset)
                              (@#'boot.core/temp-dir** nil :source)
                              (@#'boot.core/temp-dir** nil :resource)
                              (@#'boot.core/temp-dir** nil :user :asset)
                              (@#'boot.core/temp-dir** nil :user :source)
                              (@#'boot.core/temp-dir** nil :user :resource)
                              (pod/add-shutdown-hook! @#'boot.core/do-cleanup!)
                              (binding [*ns* *ns*]
                                       (ns boot.user)
                                       (in-ns 'boot.user)
                                       (require '[boot.core :refer :all])
                                       (require '[boot.task.built-in :refer :all])
                                       (load-file ~boot-file-path))
                              (boot.core/get-env))))



(defn get-lein-project-env [project-pod project-dir-path]
      (let [lein-file-path (-> (io/file project-dir-path "project.clj")
                               .getAbsolutePath)]
           (->> lein-file-path
                slurp
                read-string
                (drop 2)
                (cons :version)
                (apply hash-map))))

(defn get-project-env [project-pod project-dir-path]
      (let [lein-file (io/file project-dir-path "project.clj")
            boot-file (io/file project-dir-path "build.boot")]
           (cond (.exists boot-file)
                 (get-boot-project-env project-pod project-dir-path)
                 (.exists lein-file)
                 (get-lein-project-env project-pod project-dir-path)
                 :esle (throw
                         (RuntimeException.
                           (format
                             "Cannot find a build.boot file or project.clj file for project \"%s\""
                             (-> (io/file project-dir-path)
                                 (.getName))))))))

(deftask checkouts []
         (let [checkouts-pod (pod/make-pod {:dependencies '[[boot/core "2.0.0-rc10"]]})
               checkout-files (-> (io/file "./checkouts")
                                  .listFiles
                                  vec)
               checkout-paths (map #(.getAbsolutePath %) checkout-files)
               envs (doall
                      (for [checkout-path checkout-paths]
                        {:project-path checkout-path
                         :env (get-project-env checkouts-pod checkout-path)}))]
           (doseq [{:keys [project-path env]} envs]
             (doseq [source-paths (:source-paths env)]
               (set-env! :source-paths #(conj % (str project-path "/" source-paths)))))
           (pod/destroy-pod checkouts-pod)
           (with-pre-wrap fileset fileset)))
