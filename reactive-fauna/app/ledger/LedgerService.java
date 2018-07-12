package ledger;

import com.faunadb.client.FaunaClient;
import com.faunadb.client.query.Expr;
import com.faunadb.client.query.Language;
import com.faunadb.client.types.Value;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.futures.CompletableFuturesExtra;
import model.LedgerEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.query.Language.Class;

@Singleton
public class LedgerService {

    private String LEDGER_CLASS = "main_ledger_class";
    private String INDEX_LEDGER_BY_CLIENT_ID = "ledger_index_client_id";

    Executor executor = new ForkJoinPool();

    private FaunaClient faunaClient;
    private FaunaConfig faunaConfig;

    @Inject
    public LedgerService(MyFaunaClient faunaClient, FaunaConfig faunaConfig) throws Exception {
        this.faunaClient = faunaClient.getClient();
        this.faunaConfig = faunaConfig;
        System.out.println("LedgerService faunaClient = " + faunaClient);
        System.out.println("LedgerService faunaConfig = " + faunaConfig);

        initLedger();
    }

    public CompletableFuture<Value> addEntry(LedgerEntry ledgerEntry) throws Exception {
        Expr entryValue = Language.Value(ledgerEntry);

        ListenableFuture<Value> addEntryFuture = faunaClient.query(
            Create(
                Class(Language.Value(LEDGER_CLASS)),
                Obj("data", entryValue)
            )
        );

        return CompletableFuturesExtra.toCompletableFuture(addEntryFuture, executor);
    }

    public CompletableFuture<Collection<LedgerEntry>> all(int clientId) throws Exception {

        System.out.println("reading LedgerService all for clientId: " + clientId);

        //Lambda Variable for each ledger entry ref
        String REF_ENTRY_ID = "NXT_ENTRY";

        ListenableFuture<Value> futureQuery = faunaClient.query(
            SelectAll(Path("data", "data"),
                Map(
                    Paginate(
                        Match(Index(Language.Value(INDEX_LEDGER_BY_CLIENT_ID)), Language.Value(clientId))
                    ),
                    Lambda(Arr(Language.Value("counter"), Language.Value(REF_ENTRY_ID)), Get(Var(REF_ENTRY_ID))))
            )
        );

        // if you want to debug easily, convert stream to string and print out
        // String entriesString = allEntries.stream().map(LedgerEntry::toString).reduce("", String::concat);
        // System.out.println("read entries = " + entriesString);

        CompletableFuture<Value> valueCompletableFuture = CompletableFuturesExtra.toCompletableFuture(futureQuery);

        return valueCompletableFuture.thenApply(new Function<Value, Collection<LedgerEntry>>() {
            @Nullable
            @Override
            public Collection<LedgerEntry> apply(@Nullable Value results) {
                return results.asCollectionOf(LedgerEntry.class).get();
            }
        });

    }

    public LedgerEntry lastEntry(int clientId) throws Exception {
        Value result = faunaClient.query(
            Select(Language.Value("data"),
                Get(
                    Select(
                        Arr(Language.Value(0), Language.Value(1)),
                        Paginate(
                            Match(Index(Language.Value(INDEX_LEDGER_BY_CLIENT_ID)), Language.Value(clientId))
                        )
                    )
                )
            )
        ).get();

        LedgerEntry ledgerEntry = result.to(LedgerEntry.class).get();
        System.out.println("last ledger entry = " + ledgerEntry);
        return ledgerEntry;
    }

    private void initLedger() throws Exception {
        /*
         * Create the ledger class and index
         */
        Value classResults = faunaClient.query(
            CreateClass(
                Obj("name", Language.Value(LEDGER_CLASS))
            )
        ).get();
        System.out.println("Create Class for " + faunaConfig.getDbName() + ":\n " + classResults + "\n");


        Value uniqueConstraintIndex = faunaClient.query(
            CreateIndex(
                Obj(
                    "name", Language.Value("UNIQUE_ENTRY_CONSTRAINT"),
                    "source", Class(Language.Value(LEDGER_CLASS)),
                    "terms", Arr(Obj("field", Arr(Language.Value("data"), Language.Value("clientId")))),
                    "values", Arr(
                        Obj("field", Arr(Language.Value("data"), Language.Value("counter")))),
                    "unique", Language.Value(true)
                )
            )
        ).get();
        System.out.println("Created unique constraint index for " + faunaConfig.getDbName() + ":\n " + uniqueConstraintIndex + "\n");


        Value indexResults = faunaClient.query(
            CreateIndex(
                Obj(
                    "name", Language.Value(INDEX_LEDGER_BY_CLIENT_ID),
                    "source", Class(Language.Value(LEDGER_CLASS)),
                    "terms", Arr(Obj("field", Arr(Language.Value("data"), Language.Value("clientId")))),
                    "values", Arr(
                        Obj("field", Arr(Language.Value("data"), Language.Value("counter")), "reverse", Language.Value(true)),
                        Obj("field", Arr(Language.Value("ref"))))
                )
            )
        ).get();
        System.out.println("Created Index for " + faunaConfig.getDbName() + ":\n " + indexResults + "\n");

    }
}
