package gorsat;

import gorsat.Commands.Analysis;
import org.gorpipe.gor.model.Row;

import java.util.function.Consumer;

public class SpliteratorAdaptor extends Analysis implements Consumer<Row> {
    BatchedPipeStepIteratorAdaptor bpsAdaptor;
    Consumer<? super Row> cns;

    SpliteratorAdaptor(BatchedPipeStepIteratorAdaptor bpsAdaptor, Consumer<? super Row> cns) {
        this.bpsAdaptor = bpsAdaptor;
        this.cns = cns;
    }

    @Override
    public void process(Row r) {
        if (bpsAdaptor.nosplit && !bpsAdaptor.currentChrom.equals(r.chr)) reportWantsNoMore();
        else cns.accept(r);
    }

    @Override
    public void accept(Row row) {
        process(row);
    }

    @Override
    public void finish() {
        super.finish();
    }
}
