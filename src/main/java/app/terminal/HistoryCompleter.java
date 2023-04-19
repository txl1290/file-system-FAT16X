package app.terminal;

import org.apache.commons.lang3.StringUtils;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public class HistoryCompleter implements Completer {

    private History history;

    public HistoryCompleter(History history) {
        this.history = history;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String word = line.word();
        history.moveToFirst();
        do {
            String command = history.current();
            if(StringUtils.isNotEmpty(word) && command.startsWith(word)) {
                candidates.add(new Candidate(command));
            }
        } while (history.next());
    }
}
