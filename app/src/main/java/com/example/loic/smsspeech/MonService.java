package com.example.loic.smsspeech;

import java.util.ArrayList;
import java.util.Locale;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class MonService extends Service implements OnInitListener {
	
	Receiver receiver = new Receiver();
	private TextToSpeech mTTS;
	private String strPhrase1;
	private String strPhrase2 = "Lecture du message";
	private String strNomContact;
	private ArrayList<Appelant> liste;
	
	public class Receiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			/*Initialisation broadcastReceiver
			 * 
			 * C'est au moment ou listener d'android recoit un SMS que l'action s'effectue
			*/
			
//			Je cr�e un intent qui va recuperer le fournisseur de service qui g�re la r�ception des SMS
			
			if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
				Bundle bundle = intent.getExtras();										// Je recup�re les données de l'intent qui vient de d'arriver avec le SMS
				if(bundle != null){
					Object[] pdus = (Object[]) bundle.get("pdus");						// Je r�cup�re tous les messages bruts de la collection dans un tableau
					SmsMessage[] messages = new SmsMessage[pdus.length];				// �� me donne un tableau en deux dimensions
					for(int i=0;i<pdus.length;i++) {									
						messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);		
						for(SmsMessage message : messages){
							chercherContacts(message.getOriginatingAddress());			/* Pour chaque message que je lis, je cherche les infos du sender et je les 
																							place dans la liste et je teste 
																						*/
							if(liste.size() > 0){
								for(Appelant ap : liste){								// si le sender du message existe dans le phone alors je r�cup�re son nom
									strNomContact = ap.getoNom();
								}
								liste.clear();
							}
							else {
								strNomContact = message.getOriginatingAddress();		// sinon, je lis le num�ro de t�l�phone simplement
							}
							mTTS.setLanguage(Locale.FRENCH);							// je choisis la langue de lecture du message et je lis le message
							String strPhrase = strPhrase1 + " de " + MonService.this.getStrNomContact() + "..." + 
							strPhrase2 + "... " + message.getMessageBody() + "... fin du message";
							mTTS.speak(strPhrase,TextToSpeech.QUEUE_ADD, null);
						}
					}
				}	
			}
		}			
	}
		
	
    public void onCreate() {                
             
		mTTS = new TextToSpeech(this, this);
	}
    
    @Override
    public void onStart(Intent intent, int startId) {
    	
    }
	
	private IBinder mBinder = new MonServiceBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		
		return mBinder;
	}
	
	public void lancerService(){
		
		Toast.makeText(MonService.this, "Activation du service", Toast.LENGTH_SHORT).show();
		Toast.makeText(this, "Service activé", Toast.LENGTH_SHORT).show();
		registerReceiver(receiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
	}
	
	public void arreterService(){
		Intent intent = new Intent(this, MonService.class);
		stopService(intent);
		this.mBinder = null;
		mTTS.stop();
		mTTS.shutdown();
		unregisterReceiver(receiver);
		Toast.makeText(this, "Le service est arreté", Toast.LENGTH_LONG).show();
	}

	public class MonServiceBinder extends Binder {
		
		MonService getService(){
			return MonService.this;
		}
	}

	@Override
	public void onInit(int status) {
				
		if(status == TextToSpeech.SUCCESS){
			Toast.makeText(this, "Initialisation du service", Toast.LENGTH_SHORT);
			Toast.makeText(this, "Service initialisé", Toast.LENGTH_LONG).show();
		}
		else {
			Toast.makeText(this, "Une erreur est survenue !", Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void onDestroy() {
		Toast.makeText(this, "Le service est arreté", Toast.LENGTH_LONG).show();
		mTTS.stop();
		mTTS.shutdown();
		if(receiver != null)
			unregisterReceiver(receiver);
	}
	
//  Accesseurs pour l'attribut strPhrase
	public String getStrPhrase() {
		return strPhrase1;
	}

	public void setStrPhrase(String strPhrase) {
		this.strPhrase1 = strPhrase;
	}
	
//	Accesseurs pour l'attribut nom qui me servira � r�cup�rer le cas �ch�ant le nom
//	du contact
	
	public String getStrNomContact() {
		return strNomContact;
	}

	public void setStrNomContact(String strNomContact) {
		this.strNomContact = strNomContact;
	}
//	*************Fin des accesseurs**********
	
	private void chercherContacts(String pNumero){

//		M�thode servant � trouver le nom du contact qui envoie le message s'il existe dans notre r�pertoire
		
//		Liste servant � stoquer la liste des contacts du phone
		liste = new ArrayList<Appelant>();
		Cursor cInfosContacts = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
			
//		Je boucle sur la collection de contacts et je r�cup�re l'id du contact, son nom et son num�ro (s'ils existent)
//		pour faire la correspondance.
		
		while (cInfosContacts.moveToNext()){
			String contactId = cInfosContacts.getString(cInfosContacts.getColumnIndex(ContactsContract.Contacts._ID));
		    String nomContact = cInfosContacts.getString(cInfosContacts.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
		    String phoneExist = cInfosContacts.getString(cInfosContacts.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
		    if((contactId != null) && (nomContact != null) && (phoneExist != null)){
		    	if ( phoneExist.equalsIgnoreCase("1")){
		    		phoneExist = "true";
		    	}
		    	else
		    		phoneExist = "false" ;

		    	if (Boolean.parseBoolean(phoneExist)){
		    		Cursor numContacts = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
		    				null,ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId,null, null);
		    		while (numContacts.moveToNext()){
		    			String lNumero = numContacts.getString(numContacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
		    			if(lNumero.equals(pNumero)){
		    				Appelant ap = new Appelant();
		    				ap.setoNom(nomContact);
		    				ap.setoNum(pNumero);
		    				liste.add(ap);
		    			}
		    		}
		    		numContacts.close();
		    	}
		    }
		}
		cInfosContacts.close();
	}
}
