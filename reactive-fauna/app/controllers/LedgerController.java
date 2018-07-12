package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import ledger.LedgerService;
import model.LedgerEntry;
import play.libs.Json;
import play.mvc.Result;

import play.mvc.*;

import javax.inject.Inject;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class LedgerController extends Controller {

    private final LedgerService ledgerService;

    @Inject
    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
        System.out.println("LedgerController Start ledgerService = " + ledgerService);
    }

    public CompletionStage<Result> add() throws Exception {

        JsonNode json = request().body().asJson();
        LedgerEntry ledgerEntry = Json.fromJson(json, LedgerEntry.class);

        return ledgerService
            .addEntry(ledgerEntry)
            .thenApply(value -> ok("Stored entry:\n " + value + "\n"));
    }

    public CompletionStage<Result> all(int id) throws Exception {
        CompletableFuture<Collection<LedgerEntry>> collectionCompletableFuture = ledgerService.all(id);
        return collectionCompletableFuture.thenApply(value -> ok(Json.toJson(value)));
    }

    public Result last(int id) throws Exception {
        LedgerEntry ledgerEntry = ledgerService.lastEntry(id);

        return ok(Json.toJson(ledgerEntry));
    }
}
