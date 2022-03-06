java -cp "/home/csci8920/chordthrift/libthrift-0.15.0.jar:/home/csci8920/chordthrift/slf4j-api-1.7.9.jar:/home/csci8920/chordthrift/javax.annotation-api-1.3.2.jar:." Server -d &
java -cp "/home/csci8920/chordthrift/libthrift-0.15.0.jar:/home/csci8920/chordthrift/slf4j-api-1.7.9.jar:/home/csci8920/chordthrift/javax.annotation-api-1.3.2.jar:." NodeServer 9091 -d &
java -cp "/home/csci8920/chordthrift/libthrift-0.15.0.jar:/home/csci8920/chordthrift/slf4j-api-1.7.9.jar:/home/csci8920/chordthrift/javax.annotation-api-1.3.2.jar:." NodeServer 9092 -d &
