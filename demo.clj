(-> (org.apache.log4j.Logger/getRootLogger)
    (.setLevel org.apache.log4j.Level/ERROR))

(?<- (stdout)
     [?follower ?age]
     (follows "emily" ?follower)
     (age ?follower ?age))
