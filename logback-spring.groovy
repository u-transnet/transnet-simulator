import PatternLayoutEncoder
import FileAppender

import static Level.ALL
import static Level.DEBUG

appender("TRANSACTIONS", FileAppender) {
    file = "transactions.log"
    encoder(PatternLayoutEncoder) {
        pattern = "%date %level %msg%n"
    }
}
// Missing 'class' attribute in <appender> element
// Halting further processing of this element
root(ch.qos.logback.classic.Level.DEBUG, ["CONSOLE"])
logger("com.github.utransnet.simulator.logging.TransactionLogger", ch.qos.logback.classic.Level.ALL, ["TRANSACTIONS"])