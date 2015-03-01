(set-env! :dependencies '[[boot/core "2.0.0-rc10"]
                          [boot/worker "2.0.0-rc10"]
                          [ewen.boot/boot-maven "0.0.1"]
                          [ewen.boot/boot-misc "0.0.1"]
                          [adzerk/bootlaces "0.1.11" :scope "test"]]
          :source-paths #{"src/clj"})

(require '[ewen.boot.boot-maven :refer [gen-pom]]
         '[ewen.boot.boot-misc :refer [add-src]]
         '[adzerk.bootlaces])

(let [pom-opts {:project 'ewen.boot/boot-checkouts
                :version "0.0.1-SNAPSHOT"}]
  (task-options!
    pom pom-opts
    gen-pom pom-opts))

(deftask push-release []
         (comp (pom) (add-src) (jar) (adzerk.bootlaces/push-release)))

(deftask build-jar []
         (comp (pom) (add-src) (jar)))

(deftask install-jar []
         (comp (pom) (add-src) (jar) (install)))