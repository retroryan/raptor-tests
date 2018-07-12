package controllers;

import ledger.LedgerService;
import ledger.MyFaunaClient;
import model.LedgerEntry;
import play.mvc.*;

import javax.inject.Inject;
import java.util.Collection;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {


    private final LedgerService ledgerService;

    @Inject
    public HomeController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
        System.out.println("HomeController Start ledgerService = " + ledgerService);
    }

    public Result index() throws Exception {
        return ok("Home Controller");
    }

}
