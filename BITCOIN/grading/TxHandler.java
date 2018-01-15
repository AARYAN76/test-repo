

import java.lang.reflect.Array;
import java.security.PublicKey;
import java.util.ArrayList;

public class TxHandler {

    UTXOPool upool;


    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        upool = utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx)
    {
        try {
            // IMPLEMENT THIS
            boolean check_valid = false;
            // (1) all outputs claimed ;by {@code tx} are in the current UTXO pool,
            check_valid = isTxInPool(tx);
            //(2) the signatures on each input of {@code tx} are valid,
            check_valid = isSignatureOnTxInputs(tx);
            //(3) no UTXO is claimed multiple times by {@code tx},
            check_valid = isDoubleSpend(tx);
            // (4) all of {@code tx}s output values are non-negative
            check_valid = isTxValuesPositive(tx);
            // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
            check_valid = isInputOutputValueValid(tx);
            return check_valid;
        }
        catch(Exception e){
            return false;
        }
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // take the list check if its valid,
        // then spend the transaction by taking it out of the transaction unspent pool.
        ArrayList<Transaction> successTxs = new ArrayList();
        for (int a= 0 ;a  < possibleTxs.length; a++){

            Transaction tx = possibleTxs[a];
            if(isValidTx(tx)){
                System.out.println("this is a valid transaction, try update upool");
                for (int i = 0; i < tx.numOutputs(); i++) {
                    upool.removeUTXO(new UTXO(tx.getHash(),i));
                    successTxs.add(tx);
                }
            }
        }
        Transaction[] successTxs1 = new Transaction[successTxs.size()];
        int i = 0;
        for (Transaction sb : successTxs)
            successTxs1[i++] = sb;
        return successTxs1;

    }

    public boolean isTxInPool(Transaction tx){
        boolean check_valid = false;

        ArrayList<UTXO> list = upool.getAllUTXO();
        System.out.println("pool has "+list.size()+" unspent  transactions.output");

        System.out.println("1.) check the Transaction.Outputs claimd by tx is in unspent transaction pool:");
        for (int b = 0;b < tx.numOutputs(); b++){
            check_valid = upool.contains(new UTXO(tx.getHash(),b));

            //check the Transaction Output is Valid, and not null
            if(upool.getTxOutput(new UTXO(tx.getHash(),b))==null){
                return false;
            }


            System.out.println("\t "+b+".) tx hash "+tx.getHash()+" at block(index) "+b+", pool check ->"+check_valid);
            if(!check_valid){
                return false;
            }
        }
        return check_valid;
    }

    public boolean isSignatureOnTxInputs(Transaction tx){

        boolean check_valid = false;
        System.out.println("2.) Check "+tx.numInputs()+" Transaction.Inputs whether they have valid signatures:");
        for (int b = 0;b < tx.numInputs(); b++){
            Transaction.Input input = tx.getInput(b);
            UTXO previous_tx = new UTXO (input.prevTxHash, b);
            Transaction.Output previous_output = upool.getTxOutput(previous_tx);
            if (previous_output == null) {
                System.out.println("we are at the root there is no previous address.");
                return true;
            }
            PublicKey pk = previous_output.address;
            byte [] message = tx.getRawDataToSign(b);
            byte [] signature = input.signature;

            check_valid = Crypto.verifySignature(pk,message, signature);
            System.out.println("\t input index:"+b+".) for hash on tx "+tx.getHash()+" did it get signed? "+check_valid);
            if(!check_valid){
                return false;
            }
        }
        return check_valid;
    }

    //(3) no UTXO is claimed multiple times by {@code tx},


    public boolean isDoubleSpend(Transaction tx){
        boolean check_valid = false;

        System.out.println("Double spend check in :"+tx.numInputs()+" inputs");
        ArrayList<UTXO> list = upool.getAllUTXO();


        for (int b = 0;b < tx.numInputs(); b++){

            Transaction.Input input = tx.getInput(b);
            UTXO previous_tx = new UTXO (input.prevTxHash, input.outputIndex);

            //is this previus unspent transaction mentioned in upool more than once? or not at all?
            Transaction.Output previous_output = upool.getTxOutput(previous_tx);
            /**
             That means that you'll check the inputs of a transaction (iterate over (almost) all of them),
             and see if you can  find another input of a transaction that references to the same UTXO.
             */
            int count_double=0;
            for (UTXO utxo:list ) {
                if(previous_tx.equals(utxo)){
                    count_double++;
                }
            }
            if(count_double>1){
                return false;
            }



            System.out.println("\t Previous transaction output has value:"+ previous_output.value);
            double spend = 0;

            for(int a = 0; a < tx.numOutputs(); a++){
                Transaction.Output output = tx.getOutput(a);
                //can we spend this
                spend = spend + output.value;
                if(spend > previous_output.value){
                    System.out.println("\t spend is at "+ spend + " can only spend up to "+ previous_output.value);
                    return false;
                }

            }

            check_valid = true;
            System.out.println("\t input index:"+b+".) for hash on tx "+tx.getHash()+" are we spending this only once? "+check_valid);

        }
        return check_valid;
    }

    public boolean isTxValuesPositive(Transaction tx){
        boolean check_valid = false;
        System.out.println("check if there are any negative values");
        for (int b = 0;b < tx.numOutputs(); b++){
            Transaction.Output output = upool.getTxOutput(new UTXO(tx.getHash(),b));
            System.out.println("\t "+b+".) hash of transaction is "+tx.getHash()+" at index "+b+", has value ->"+output.value);
            if(output.value<0){
                return false;
            }
        }
        return check_valid;
    }

    public boolean isInputOutputValueValid(Transaction tx){
        boolean check_valid = false;

        //get previous transaction, and check the output value.
        System.out.println("check the input values are greater than or equal to the sum of its output");
        double input_values = 0;
        for (int b = 0;b < tx.numInputs(); b++) {
            Transaction.Input input = tx.getInput(b);
            Transaction.Output output = upool.getTxOutput(new UTXO(input.prevTxHash, b));
            System.out.println("\t "+b+".) hash of input transactions is "+input.prevTxHash+" at index "+b+", has value ->"+output.value);
            input_values=input_values+output.value;
        }

        double output_values = 0;
        for (int b = 0;b < tx.numOutputs(); b++){
            Transaction.Output output = upool.getTxOutput(new UTXO(tx.getHash(),b));
            System.out.println("\t "+b+".) hash of output transaction is "+tx.getHash()+" at index "+b+", has value ->"+output.value);
            output_values = output_values + output.value;
        }
        if(input_values>=output_values){
            System.out.println("were good");
            check_valid = true;
            return check_valid;
        }
        return check_valid;
    }
}
