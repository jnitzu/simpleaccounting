package joni.lehtinen.fi.simpleaccounting;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Joni Lehtinen on 16.10.2015.
 */
public class AccountProvider extends ContentProvider {

    private static final String TAG = "AccountProvider";

    public enum ACCOUNT {
        ID("_id"),
        TITLE("title"),
        DESCRIPTION("description"),
        TYPE_ID("type_id"),
        BALANCE("balance");

        public final String SQL_NAME;

        ACCOUNT(String name){
            SQL_NAME = name;
        }

        @Override
        public String toString() {
            return SQL_NAME;
        }

        public String fullName() {
            return TABLE_NAME_ACCOUNT + "." + SQL_NAME;
        }
    }

    public enum ACCOUNT_TYPE {
        ID("_id"),
        TYPE("type");

        public final String SQL_NAME;

        ACCOUNT_TYPE(String name){
            SQL_NAME = name;
        }

        @Override
        public String toString() {
            return SQL_NAME;
        }

        public String fullName() {
            return TABLE_NAME_ACCOUNT_TYPE + "." + SQL_NAME;
        }
    }

    public enum PAYMENT {
        ID("_id"),
        TITLE("title"),
        LABEL_ID("label_id"),
        AMOUNT("amount"),
        ACCOUNT_ID("account_id"),
        PAYMENT_DATE("payment_date"),
        DESCRIPTION("description");

        public final String SQL_NAME;

        PAYMENT(String name){
            SQL_NAME = name;
        }

        @Override
        public String toString() {
            return SQL_NAME;
        }

        public String fullName() {
            return TABLE_NAME_PAYMENT + "." + SQL_NAME;
        }
    }

    public enum INVOICE {
        ID("_id"),
        DUE_DATE("due_date"),
        AUTO_PAY_DATE("auto_pay_date"),
        REPEAT_INTERVAL("repeat_interval"),
        NOTIFICATION_DATE("notification_date");

        public final String SQL_NAME;

        INVOICE(String name){
            SQL_NAME = name;
        }

        @Override
        public String toString() {
            return SQL_NAME;
        }

        public String fullName() {
            return TABLE_NAME_INVOICE + "." + SQL_NAME;
        }
    }

    public enum PAYMENT_LABEL {
        ID("_id"),
        LABEL("label");

        public final String SQL_NAME;

        PAYMENT_LABEL(String name){
            SQL_NAME = name;
        }

        @Override
        public String toString() {
            return SQL_NAME;
        }

        public String fullName() {
            return TABLE_NAME_PAYMENT_LABEL + "." + SQL_NAME;
        }
    }

    /**
     * Database specific constant declarations
     */
    private SQLiteDatabase db;
    static final String DATABASE_NAME = "simple_accounting";
    static final String TABLE_NAME_ACCOUNT = "account";
    static final String TABLE_NAME_ACCOUNT_TYPE = "account_type";
    static final String TABLE_NAME_INVOICE = "invoice";
    static final String TABLE_NAME_PAYMENT = "payment";
    static final String TABLE_NAME_PAYMENT_LABEL = "payment_label";
    static final String TABLE_DEBUG = "debug";
    private static final String TRIGGER_NAME_PAYMENT_INSERT = "insert_payment";
    private static final String TRIGGER_NAME_PAYMENT_DELETE = "delete_payment";
    private static final String TRIGGER_NAME_PAYMENT_UPDATE_SET_PAID = "update_payment_paid";
    private static final String TRIGGER_NAME_PAYMENT_UPDATE_SET_UNPAID = "update_payment_unpaid";
    private static final String TRIGGER_NAME_PAYMENT_UPDATE_AMOUNT_CHANGED = "update_payment_amount_changed";
    private static final String TRIGGER_NAME_PAYMENT_UPDATE_ACCOUNT_CHANGED = "update_payment_amount_account_changed";
    static final int DATABASE_VERSION = 19;

    /**
     * ContentProvider specific constant declarations
     */
    public static final String PROVIDER_NAME = "fi.lehtinen.provider.account";
    public static final String URL = "content://" + PROVIDER_NAME + "/";
    public static final Uri CONTENT_URI_ACCOUNT = Uri.parse(URL + TABLE_NAME_ACCOUNT);
    public static final Uri CONTENT_URI_PAYMENT = Uri.parse(URL + TABLE_NAME_PAYMENT);
    public static final Uri CONTENT_URI_INVOICE = Uri.parse(URL + TABLE_NAME_INVOICE);
    public static final Uri CONTENT_URI_PAYMENT_LABEL = Uri.parse(URL + TABLE_NAME_PAYMENT_LABEL);
    public static final Uri CONTENT_URI_ACCOUNT_TYPE = Uri.parse(URL + TABLE_NAME_ACCOUNT_TYPE);
    public static final Uri CONTENT_URI_DEBUG = Uri.parse(URL + TABLE_DEBUG);

    private static final int ACCOUNTS = 1;
    private static final int ACCOUNTS_ID = 2;
    private static final int PAYMENTS = 3;
    private static final int PAYMENTS_ID = 4;
    private static final int INVOICES = 5;
    private static final int INVOICES_ID = 6;
    private static final int PAYMENT_LABELS = 7;
    private static final int PAYMENT_LABELS_ID = 8;
    private static final int ACCOUNT_TYPES = 9;
    private static final int DEBUG = 100;

    private static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, TABLE_NAME_ACCOUNT, ACCOUNTS);
        uriMatcher.addURI(PROVIDER_NAME, TABLE_NAME_ACCOUNT + "/#", ACCOUNTS_ID);
        uriMatcher.addURI(PROVIDER_NAME, TABLE_NAME_PAYMENT, PAYMENTS);
        uriMatcher.addURI(PROVIDER_NAME, TABLE_NAME_PAYMENT + "/#", PAYMENTS_ID);
        uriMatcher.addURI(PROVIDER_NAME, TABLE_NAME_INVOICE, INVOICES);
        uriMatcher.addURI(PROVIDER_NAME, TABLE_NAME_INVOICE + "/#", INVOICES_ID);
        uriMatcher.addURI(PROVIDER_NAME, TABLE_NAME_PAYMENT_LABEL, PAYMENT_LABELS);
        uriMatcher.addURI(PROVIDER_NAME, TABLE_NAME_PAYMENT_LABEL + "/#", PAYMENT_LABELS_ID);
        uriMatcher.addURI(PROVIDER_NAME, TABLE_NAME_ACCOUNT_TYPE, ACCOUNT_TYPES);
        uriMatcher.addURI(PROVIDER_NAME, TABLE_DEBUG, DEBUG);
    }

    /**
     * Helper class that actually creates and manages
     * the provider's underlying data repository.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db){

            // Enable foreign keys
            db.execSQL("PRAGMA foreign_keys = ON;");

            db.execSQL("CREATE TABLE " + TABLE_NAME_ACCOUNT_TYPE + " (" +
                    ACCOUNT_TYPE.ID + " INTEGER, " +
                    ACCOUNT_TYPE.TYPE + " TEXT NOT NULL, " +
                    "PRIMARY KEY(" + ACCOUNT_TYPE.ID + " ASC)" +
                    ");");

            db.compileStatement("INSERT INTO " + TABLE_NAME_ACCOUNT_TYPE + "(" + ACCOUNT_TYPE.TYPE + ") VALUES (\"Wallet\")").executeInsert();
            db.compileStatement("INSERT INTO " + TABLE_NAME_ACCOUNT_TYPE + "(" + ACCOUNT_TYPE.TYPE + ") VALUES (\"Debit\")").executeInsert();
            db.compileStatement("INSERT INTO " + TABLE_NAME_ACCOUNT_TYPE + "(" + ACCOUNT_TYPE.TYPE + ") VALUES (\"Credit\")").executeInsert();

            db.execSQL("CREATE TABLE " + TABLE_NAME_ACCOUNT + " (" +
                    ACCOUNT.ID + " INTEGER, " +
                    ACCOUNT.TITLE + " TEXT NOT NULL, " +
                    ACCOUNT.DESCRIPTION + " TEXT, " +
                    ACCOUNT.TYPE_ID + " INTEGER NOT NULL, " +
                    ACCOUNT.BALANCE + " REAL NOT NULL, " +
                    "PRIMARY KEY(" + ACCOUNT.ID + " ASC), " +
                    "FOREIGN KEY(" + ACCOUNT.TYPE_ID + ") REFERENCES " + TABLE_NAME_ACCOUNT_TYPE + "(" + ACCOUNT_TYPE.ID + ")" +
                    ");");

            db.execSQL("CREATE TABLE " + TABLE_NAME_PAYMENT_LABEL + " (" +
                    PAYMENT_LABEL.ID + " INTEGER, " +
                    PAYMENT_LABEL.LABEL + " TEXT NOT NULL, " +
                    "PRIMARY KEY(" + PAYMENT_LABEL.ID + " ASC)" +
                    ");");

            db.compileStatement("INSERT INTO " + TABLE_NAME_PAYMENT_LABEL + " VALUES ( 0,\"None\")").executeInsert();

            db.execSQL("CREATE TABLE " + TABLE_NAME_PAYMENT + " (" +
                    PAYMENT.ID + " INTEGER, " +
                    PAYMENT.TITLE + " TEXT NOT NULL, " +
                    PAYMENT.LABEL_ID + " INTEGER DEFAULT 0 NOT NULL, " +
                    PAYMENT.AMOUNT + " REAL NOT NULL, " +
                    PAYMENT.ACCOUNT_ID + " INTEGER NOT NULL, " +
                    PAYMENT.PAYMENT_DATE + " INTEGER, " +
                    PAYMENT.DESCRIPTION + " TEXT, " +
                    "PRIMARY KEY(" + PAYMENT.ID + " ASC), " +
                    "FOREIGN KEY(" + PAYMENT.LABEL_ID + ") REFERENCES " + TABLE_NAME_PAYMENT_LABEL + "(" + PAYMENT_LABEL.ID + ") ON DELETE SET DEFAULT, " +
                    "FOREIGN KEY(" + PAYMENT.ACCOUNT_ID + ") REFERENCES " + TABLE_NAME_ACCOUNT + "(" + ACCOUNT.ID + ") ON DELETE CASCADE" +
                    ");");

            db.execSQL("CREATE TABLE " + TABLE_NAME_INVOICE + " (" +
                    INVOICE.ID + " INTEGER, " +
                    INVOICE.DUE_DATE + " INTEGER NOT NULL, " +
                    INVOICE.AUTO_PAY_DATE + " INTEGER, " +
                    INVOICE.REPEAT_INTERVAL + " INTEGER, " +
                    INVOICE.NOTIFICATION_DATE + " INTEGER, " +
                    "PRIMARY KEY(" + INVOICE.ID + " ASC), " +
                    "FOREIGN KEY(" + INVOICE.ID + ") REFERENCES " + TABLE_NAME_PAYMENT + "(" + PAYMENT.ID + ") ON DELETE CASCADE" +
                    ");");

            db.execSQL("CREATE TRIGGER " + TRIGGER_NAME_PAYMENT_INSERT + " " +
                    "AFTER INSERT ON " + TABLE_NAME_PAYMENT + " " +
                    "WHEN NEW." + PAYMENT.PAYMENT_DATE + " NOT NULL " +
                    "BEGIN " +
                    "UPDATE " + TABLE_NAME_ACCOUNT + " SET " + ACCOUNT.BALANCE + " = " + ACCOUNT.BALANCE + " - NEW." + PAYMENT.AMOUNT + " " +
                    "WHERE " + ACCOUNT.ID + " = NEW." + PAYMENT.ACCOUNT_ID + "; " +
                    "END;");

            db.execSQL("CREATE TRIGGER " + TRIGGER_NAME_PAYMENT_DELETE + " " +
                    "BEFORE DELETE ON " + TABLE_NAME_PAYMENT + " " +
                    "WHEN OLD." + PAYMENT.PAYMENT_DATE + " NOT NULL " +
                    "BEGIN " +
                    "UPDATE " + TABLE_NAME_ACCOUNT + " SET " + ACCOUNT.BALANCE + " = " + ACCOUNT.BALANCE + " + OLD." + PAYMENT.AMOUNT + " " +
                    "WHERE " + ACCOUNT.ID + " = OLD." + PAYMENT.ACCOUNT_ID + "; " +
                    "END;");

            db.execSQL("CREATE TRIGGER " + TRIGGER_NAME_PAYMENT_UPDATE_SET_PAID + " " +
                    "BEFORE UPDATE ON " + TABLE_NAME_PAYMENT + " " +
                    "WHEN OLD." + PAYMENT.PAYMENT_DATE + " IS NULL AND NEW." + PAYMENT.PAYMENT_DATE + " NOT NULL " +
                    "BEGIN " +
                    "UPDATE " + TABLE_NAME_ACCOUNT + " SET " + ACCOUNT.BALANCE + " = " + ACCOUNT.BALANCE + " - NEW." + PAYMENT.AMOUNT + " " +
                    "WHERE " + ACCOUNT.ID + " = NEW." + PAYMENT.ACCOUNT_ID + "; " +
                    "END;");

            db.execSQL("CREATE TRIGGER " + TRIGGER_NAME_PAYMENT_UPDATE_SET_UNPAID + " " +
                    "BEFORE UPDATE ON " + TABLE_NAME_PAYMENT + " " +
                    "WHEN OLD." + PAYMENT.PAYMENT_DATE + " NOT NULL AND NEW." + PAYMENT.PAYMENT_DATE + " IS NULL " +
                    "BEGIN " +
                    "UPDATE " + TABLE_NAME_ACCOUNT + " SET " + ACCOUNT.BALANCE + " = " + ACCOUNT.BALANCE + " + OLD." + PAYMENT.AMOUNT + " " +
                    "WHERE " + ACCOUNT.ID + " = OLD." + PAYMENT.ACCOUNT_ID + "; " +
                    "END;");

            db.execSQL("CREATE TRIGGER " + TRIGGER_NAME_PAYMENT_UPDATE_AMOUNT_CHANGED + " " +
                    "BEFORE UPDATE ON " + TABLE_NAME_PAYMENT + " " +
                    "WHEN OLD." + PAYMENT.PAYMENT_DATE + " NOT NULL " +
                    "AND NEW." + PAYMENT.PAYMENT_DATE + " NOT NULL " +
                    "AND OLD." + PAYMENT.AMOUNT + " IS NOT NEW." + PAYMENT.AMOUNT + " " +
                    "AND OLD." + PAYMENT.ACCOUNT_ID + " IS NEW." + PAYMENT.ACCOUNT_ID + " " +
                    "BEGIN " +
                    "UPDATE " + TABLE_NAME_ACCOUNT + " SET " + ACCOUNT.BALANCE + " = " + ACCOUNT.BALANCE + " + OLD." + PAYMENT.AMOUNT + " - NEW." + PAYMENT.AMOUNT + " " +
                    "WHERE " + ACCOUNT.ID + " = OLD." + PAYMENT.ACCOUNT_ID + "; " +
                    "END;");

            db.execSQL("CREATE TRIGGER " + TRIGGER_NAME_PAYMENT_UPDATE_ACCOUNT_CHANGED + " " +
                    "BEFORE UPDATE ON " + TABLE_NAME_PAYMENT + " " +
                    "WHEN OLD." + PAYMENT.PAYMENT_DATE + " NOT NULL " +
                    "AND NEW." + PAYMENT.PAYMENT_DATE + " NOT NULL " +
                    "AND OLD." + PAYMENT.ACCOUNT_ID + " IS NOT NEW." + PAYMENT.ACCOUNT_ID + " " +
                    "BEGIN " +
                    "UPDATE " + TABLE_NAME_ACCOUNT + " SET " + ACCOUNT.BALANCE + " = " + ACCOUNT.BALANCE + " + OLD." + PAYMENT.AMOUNT + " " +
                    "WHERE " + ACCOUNT.ID + " = OLD." + PAYMENT.ACCOUNT_ID + "; " +
                    "UPDATE " + TABLE_NAME_ACCOUNT + " SET " + ACCOUNT.BALANCE + " = " + ACCOUNT.BALANCE + " - NEW." + PAYMENT.AMOUNT + " " +
                    "WHERE " + ACCOUNT.ID + " = NEW." + PAYMENT.ACCOUNT_ID + "; " +
                    "END;");

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_INVOICE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_PAYMENT);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_ACCOUNT);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_ACCOUNT_TYPE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_PAYMENT_LABEL);
            db.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_NAME_PAYMENT_INSERT);
            db.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_NAME_PAYMENT_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_NAME_PAYMENT_UPDATE_SET_PAID);
            db.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_NAME_PAYMENT_UPDATE_SET_UNPAID);
            db.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_NAME_PAYMENT_UPDATE_AMOUNT_CHANGED);
            db.execSQL("DROP TRIGGER IF EXISTS " + TRIGGER_NAME_PAYMENT_UPDATE_ACCOUNT_CHANGED);
            onCreate(db);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);

            // Enable foreign keys
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    }

    @Override
    public boolean onCreate() {
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());

        /**
         * Create a write able database which will trigger its
         * creation if it doesn't already exist.
         */
        db = dbHelper.getWritableDatabase();
        return db != null;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)){

            case ACCOUNTS_ID:
                qb.appendWhere(ACCOUNT.ID.fullName() + "=" + uri.getPathSegments().get(1));
            case ACCOUNTS:
                qb.setTables(TABLE_NAME_ACCOUNT +
                        " JOIN " +
                        TABLE_NAME_ACCOUNT_TYPE +
                        " ON " + ACCOUNT.TYPE_ID.fullName() + "=" + ACCOUNT_TYPE.ID.fullName());
                break;
            case PAYMENTS_ID:
                qb.appendWhere(PAYMENT.ID.fullName() + "=" + uri.getPathSegments().get(1));
            case PAYMENTS:
                qb.setTables(TABLE_NAME_PAYMENT +
                        " JOIN " +
                        TABLE_NAME_PAYMENT_LABEL +
                        " ON " + PAYMENT.LABEL_ID.fullName() + "=" + PAYMENT_LABEL.ID.fullName() +
                        " JOIN " +
                        TABLE_NAME_ACCOUNT +
                        " ON " + PAYMENT.ACCOUNT_ID.fullName() + "=" + ACCOUNT.ID.fullName());
                break;
            case INVOICES_ID:
                qb.appendWhere(INVOICE.ID.fullName() + "=" + uri.getPathSegments().get(1));
            case INVOICES:
                qb.setTables(
                        TABLE_NAME_PAYMENT +
                        " JOIN " +
                        TABLE_NAME_INVOICE +
                        " ON "+ PAYMENT.ID.fullName() + "=" + INVOICE.ID.fullName() +
                        " JOIN " +
                        TABLE_NAME_PAYMENT_LABEL +
                        " ON " + PAYMENT.LABEL_ID.fullName() + "=" + PAYMENT_LABEL.ID.fullName() +
                        " JOIN " +
                        TABLE_NAME_ACCOUNT +
                        " ON " + PAYMENT.ACCOUNT_ID.fullName() + "=" + ACCOUNT.ID.fullName());
                break;
            case PAYMENT_LABELS_ID:
                qb.appendWhere(PAYMENT_LABEL.ID.fullName() + "=" + uri.getPathSegments().get(1));
            case PAYMENT_LABELS:
                qb.setTables(TABLE_NAME_PAYMENT_LABEL);
                break;
            case ACCOUNT_TYPES:
                qb.setTables(TABLE_NAME_ACCOUNT_TYPE);
                break;
            case DEBUG:
                Cursor c;
                c = db.query(TABLE_NAME_ACCOUNT,null,null,null,null,null,null);
                c.moveToFirst();
                Log.d("DEBUG_TAG", "----------------- ACCOUNT TABLE ---------------------");
                do{
                    Log.d("DEBUG_TAG",c.getInt(0) + " : " + c.getString(1) + " : " + c.getString(2) + " : " + c.getInt(3) + " : " + c.getDouble(4));
                }while(c.moveToNext());
                c.close();
                c = db.query(TABLE_NAME_PAYMENT,null,null,null,null,null,null);
                c.moveToFirst();
                Log.d("DEBUG_TAG", "----------------- PAYMENT TABLE ---------------------");
                do{
                    Log.d("DEBUG_TAG",c.getInt(0) + " : " + c.getString(1) + " : " + c.getInt(2) + " : " + c.getDouble(3) + " : " + c.getInt(4));
                } while (c.moveToNext());
                c.close();
                c = db.query(TABLE_NAME_INVOICE,null,null,null,null,null,null);
                c.moveToFirst();
                Log.d("DEBUG_TAG", "----------------- INVOICE TABLE ---------------------");
                do{
                    Log.d("DEBUG_TAG",c.getInt(0) + " : " + DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(c.getLong(1))));
                }while(c.moveToNext());
                c.close();
                return null;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        Cursor c = qb.query(db,	projection,	selection, selectionArgs,null, null, sortOrder);

        /**
         * register to watch a content URI for changes
         */
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){

            case ACCOUNTS:
                return "vnd.android.cursor.dir/vnd.fi.lehtinen.accounting." + TABLE_NAME_ACCOUNT;
            case ACCOUNTS_ID:
                return "vnd.android.cursor.item/vnd.fi.lehtinen.accounting." + TABLE_NAME_ACCOUNT;
            case PAYMENTS:
                return "vnd.android.cursor.dir/vnd.fi.lehtinen.accounting." + TABLE_NAME_PAYMENT;
            case PAYMENTS_ID:
                return "vnd.android.cursor.item/vnd.fi.lehtinen.accounting." + TABLE_NAME_PAYMENT;
            case INVOICES:
                return "vnd.android.cursor.dir/vnd.fi.lehtinen.accounting." + TABLE_NAME_INVOICE;
            case INVOICES_ID:
                return "vnd.android.cursor.item/vnd.fi.lehtinen.accounting." + TABLE_NAME_INVOICE;
            case PAYMENT_LABELS:
                return "vnd.android.cursor.dir/vnd.fi.lehtinen.accounting." + TABLE_NAME_PAYMENT_LABEL;
            case ACCOUNT_TYPES:
                return "vnd.android.cursor.dir/vnd.fi.lehtinen.accounting." + TABLE_NAME_ACCOUNT_TYPE;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID;
        String table_name;

        switch (uriMatcher.match(uri)){

            case ACCOUNTS:
                table_name = TABLE_NAME_ACCOUNT;
                break;
            case PAYMENTS:
                table_name = TABLE_NAME_PAYMENT;
                break;
            case INVOICES:
                table_name = TABLE_NAME_INVOICE;
                break;
            case PAYMENT_LABELS:
                table_name = TABLE_NAME_PAYMENT_LABEL;
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }


        rowID = db.insert(table_name, "", values);

        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(uri, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);

            // Notify that account table might have been changed by trigger after inserting to payment table
            if(uriMatcher.match(uri) == PAYMENTS)
                getContext().getContentResolver().notifyChange(CONTENT_URI_ACCOUNT, null);

            return _uri;
        }

        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){

            case ACCOUNTS:
                count = db.delete(TABLE_NAME_ACCOUNT, selection, selectionArgs);
                break;
            case ACCOUNTS_ID:
                count = db.delete( TABLE_NAME_ACCOUNT, ACCOUNT.ID +  " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            case PAYMENTS:
                count = db.delete( TABLE_NAME_PAYMENT, selection, selectionArgs);
                break;
            case PAYMENTS_ID:
                count = db.delete( TABLE_NAME_PAYMENT, PAYMENT.ID +  " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            case INVOICES:
                count = db.delete( TABLE_NAME_INVOICE, selection, selectionArgs);
                break;
            case INVOICES_ID:
                count = db.delete( TABLE_NAME_INVOICE, INVOICE.ID +  " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            case PAYMENT_LABELS:
                count = db.delete( TABLE_NAME_PAYMENT_LABEL, selection, selectionArgs);
                break;
            case PAYMENT_LABELS_ID:
                count = db.delete( TABLE_NAME_PAYMENT_LABEL, PAYMENT_LABEL.ID +  " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        // Notify that account table might have been changed by trigger after deleting from payment table
        if(uriMatcher.match(uri) == PAYMENTS || uriMatcher.match(uri) == PAYMENTS_ID)
            getContext().getContentResolver().notifyChange(CONTENT_URI_ACCOUNT, null);

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){

            case ACCOUNTS:
                count = db.update(TABLE_NAME_ACCOUNT, values, selection, selectionArgs);
                break;
            case ACCOUNTS_ID:
                count = db.update(TABLE_NAME_ACCOUNT, values, ACCOUNT.ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +selection + ')' : ""), selectionArgs);
                break;
            case PAYMENTS:
                count = db.update(TABLE_NAME_PAYMENT, values, selection, selectionArgs);
                break;
            case PAYMENTS_ID:
                count = db.update(TABLE_NAME_PAYMENT, values, PAYMENT.ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +selection + ')' : ""), selectionArgs);
                break;
            case INVOICES:
                count = db.update(TABLE_NAME_INVOICE, values, selection, selectionArgs);
                break;
            case INVOICES_ID:
                count = db.update(TABLE_NAME_INVOICE, values, INVOICE.ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +selection + ')' : ""), selectionArgs);
                break;
            case PAYMENT_LABELS:
                count = db.update(TABLE_NAME_PAYMENT_LABEL, values, selection, selectionArgs);
                break;
            case PAYMENT_LABELS_ID:
                count = db.update(TABLE_NAME_PAYMENT_LABEL, values, PAYMENT_LABEL.ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        // Notify that account table might have been changed by trigger after updating payment table
        if(uriMatcher.match(uri) == PAYMENTS || uriMatcher.match(uri) == PAYMENTS_ID)
            getContext().getContentResolver().notifyChange(CONTENT_URI_ACCOUNT, null);

        return count;
    }
}
