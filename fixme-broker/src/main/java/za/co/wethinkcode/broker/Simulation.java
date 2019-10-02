package za.co.wethinkcode.broker;

import lombok.Data;
import za.co.wethinkcode.helpers.Instrument;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

@Data
public class Simulation implements  Runnable{

    private Broker broker;


    private int simulationId;
    private static Transaction transaction;
    private static String transactionString;
    private static String marketId;
    private static String buyOrSell;
    private static String instrument;
    private static String quantity;

    private int cycles;


    public Simulation(int cycles, int id) throws IOException {
        simulationId = id;
        this.cycles = cycles;
        broker = new Broker(id);
        broker.sent = cycles;
    }

    public  void generateTransaction() {

        marketId = getRandomMarket();
        instrument = getRandomInstrument(marketId);
        buyOrSell = !brokerHasInstrument(instrument)
                ? "buy"
                : ThreadLocalRandom.current().nextInt(0, 1) == 1
                ? "buy"
                : "sell";
        quantity = getRandomQuantity(buyOrSell);

        try {
            transactionString = marketId + " "
                    + buyOrSell + " "
                    + instrument + " "
                    + quantity;
            transaction = Transaction.buildTransaction(transactionString);
        } catch (InvalidInputException e) {
            System.out.println(e.getMessage());
        }
    }

    public  String getRandomMarket() {
        int random = ThreadLocalRandom.current().nextInt(0, broker.getMarketInstruments().getInstruments().size());
        return broker.getMarketInstruments().getInstruments().keySet().toArray()[random].toString();
    }

    public  String getRandomInstrument(String marketId) {
        int random = ThreadLocalRandom.current().nextInt(0, broker.getMarketInstruments().getInstruments().get(marketId).size());
        return broker.getMarketInstruments().getInstruments().get(marketId).get(random).instrument;
    }

    public  boolean brokerHasInstrument(String instrument) {
       List<Instrument> instr =  broker.getMarketInstruments().getInstruments().get(marketId);
        for (Instrument value : instr) {
           // System.out.println("HERE " + value.instrument + " " + value.reserveQty);
            if (value.instrument.equals(instrument)) {
                if (value.reserveQty > 0)
                    return true;
            }
        }
        return false;
    }

    public  String getRandomQuantity(String buyOrSell) {
        List<Instrument> instr = broker.getMarketInstruments().getInstruments().get(marketId);

        if (buyOrSell.equals("buy")) {
            return String.format("%d", ThreadLocalRandom.current().nextInt(1, 100000));
        } else {
            for (Instrument value : instr) {
                if (value.instrument.equals(instrument)) {
                    if (value.reserveQty > 0)
                    return String.format("%d", ThreadLocalRandom.current().nextInt(value.reserveQty));
                }
            }
        }
        return "0";
    }

    @Override
    public void run() {


        Thread thread = new Thread(() -> {

            while (true) {

                try {

                    broker.processResponse(simulationId);
                    if (broker.sentIsReceived()) {
                        broker.getSocket().close();
                        System.out.println("SIMULATION DONE!");
                        break;
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                } catch (NoSuchElementException e) {
                        broker.killBroker();
                }
            }
        });
        thread.start();

        int i = 0;

        while (i < cycles) {

            if (broker.getMarketInstruments().getInstruments().size() > 0) {
                generateTransaction();
                try {
                    System.out.println("Thread " + simulationId + " Request to Market:\n" + transactionString + "\n");
                    broker.sendMessage(transaction);
                    System.out.println("Thread " + simulationId + " Waiting for Response from Market...\n");
                } catch (IOException | NoSuchElementException e) {
                    System.out.println("Thread " + simulationId + " Error:\n" + e.getMessage() + "\n");
                }
                i++;
            }
        }
        System.out.println("CYCLES DONE");
    }



}
