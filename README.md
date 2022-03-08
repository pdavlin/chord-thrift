# Running this project

Java code must first be compiled with the javac command. 

Then, each component can be started with the following call configuration. 

***Note:** that in the development environment, the project was stored on the `/home/csci8920/chordthrift/gen-java/shared` directory, and the required .jar files were in the `/home/csci8920/chordthrift` directory. The full call stack is included in the `start_test_servers.sh` file, but must be run on the command line because shell scripts do not support I/O to Java. Individual components of the system can also be called directly using those commands.*

```
java -cp "/home/csci8920/chordthrift/libthrift-0.15.0.jar:/home/csci8920/chordthrift/slf4j-api-1.7.9.jar:/home/csci8920/chordthrift/javax.annotation-api-1.3.2.jar:." Server -d &
java -cp "/home/csci8920/chordthrift/libthrift-0.15.0.jar:/home/csci8920/chordthrift/slf4j-api-1.7.9.jar:/home/csci8920/chordthrift/javax.annotation-api-1.3.2.jar:." NodeServer 9091 -d &
java -cp "/home/csci8920/chordthrift/libthrift-0.15.0.jar:/home/csci8920/chordthrift/slf4j-api-1.7.9.jar:/home/csci8920/chordthrift/javax.annotation-api-1.3.2.jar:." NodeServer 9092 -d &
java -cp "/home/csci8920/chordthrift/libthrift-0.15.0.jar:/home/csci8920/chordthrift/slf4j-api-1.7.9.jar:/home/csci8920/chordthrift/javax.annotation-api-1.3.2.jar:." Client -d 

