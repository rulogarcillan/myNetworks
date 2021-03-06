package com.r.raul.tools.Ports;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.r.raul.tools.R;
import com.r.raul.tools.Utils.Connectivity;
import com.r.raul.tools.Utils.Constantes;
import com.r.raul.tools.Utils.LogUtils;

import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainOpenPorts extends Fragment {


    public static MainOpenPorts newInstance() {
        MainOpenPorts fragment = new MainOpenPorts();
        return fragment;
    }

    public MainOpenPorts() {

    }


    ObtenerIp task = new ObtenerIp();
    RecyclerView botoneraRecycler;
    TextView txtPorst, txtIpHost;
    Spinner spinner;
    FloatingActionButton btnAceptar;
    Boolean resultadoParseaPorts = false;


    public void onPause() {
        task.cancel(true);
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        View rootView = inflater.inflate(R.layout.open_ports, container, false);

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.ports_m);
        DrawerLayout drawer = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(getActivity(), drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        txtIpHost = (TextView) rootView.findViewById(R.id.txtIpHost);

        btnAceptar = (FloatingActionButton) rootView.findViewById(R.id.btnAceptar);
        spinner = (Spinner) rootView.findViewById(R.id.spinner);
        txtPorst = (TextView) rootView.findViewById(R.id.txtPorst);
        botoneraRecycler = (RecyclerView) rootView.findViewById(R.id.botoneraRecycler);
        botoneraRecycler.setHasFixedSize(true);

        //la ultima para que este todo inicializado
        setupUI(rootView.findViewById(R.id.contenedor));

        final BotoneraAdapter adaptador = new BotoneraAdapter(getActivity()) {
            @Override
            public void onBindViewHolder(Holder holder, final int position) {
                super.onBindViewHolder(holder, position);
                holder.btnPorts.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        txtPorst.setText(array.get(position).getRangoPuertos());
                    }
                });
            }
        };


        botoneraRecycler.setAdapter(adaptador);
        botoneraRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));

        loadSnniper();
        txtPorst.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                txtPorst.setSingleLine(!hasFocus);
            }
        });

        //acepatr, valida y si ok nueva intent
        btnAceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String datos[] = new String[2];
                datos[0] = txtPorst.getText().toString();
                datos[1] = txtIpHost.getText().toString();


                if (Connectivity.isConnected(getActivity())) {
                    new ObtenerIp(getActivity()) {
                        @Override
                        protected void onPostExecute(String ip) {
                            super.onPostExecute(ip);
                            if (!isCancelled()) {

                                if (!new InetAddressValidator().isValidInet4Address(ip)) {
                                    aviso(R.string.ipnovalida);

                                } else if (!resultadoParseaPorts) {
                                    aviso(R.string.puertosnovalidos);
                                } else {
                                    int time = Integer.parseInt(spinner.getSelectedItem().toString());
                                    String port = txtPorst.getText().toString();
                                    //  new AnalizarPuertos(getActivity(),ip,time).execute(listaPuertos);

                                    Intent myIntent = new Intent(getActivity(), DetallePuertos.class);
                                    myIntent.putExtra(Constantes.IP, ip); //Ip
                                    myIntent.putExtra(Constantes.TIMEOUT, time); //Timeout
                                    myIntent.putExtra(Constantes.PORTS, port); //Ports
                                    getActivity().startActivity(myIntent);
                                }
                            }
                        }
                    }.execute(datos);
                } else {
                    aviso(R.string.necesitaInet);
                }

            }
        });


        btnAceptar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (keyboardShown(btnAceptar.getRootView())) {
                    btnAceptar.hide();
                } else {
                    btnAceptar.show();
                }
            }
        });

        return rootView;
    }

    private boolean keyboardShown(View rootView) {

        final int softKeyboardHeight = 100;
        Rect r = new Rect();
        rootView.getWindowVisibleDisplayFrame(r);
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
        int heightDiff = rootView.getBottom() - r.bottom;
        return heightDiff > softKeyboardHeight * dm.density;
    }

    public boolean parsea(String lista) {
        ArrayList<Integer> listaPuertos = new ArrayList<Integer>();
        Set<Integer> hs = new HashSet<>();
        lista = lista.replaceAll("[^0-9,-]", "");
        lista = lista.replace(" ", "");
        String[] partsComa = lista.split(",");
        if (lista.equals("")) {
            return false;
        }
        for (String portsComa : partsComa) {

            if (portsComa.contains("-")) {
                String[] partsGuion = portsComa.split("\\-");

                if (partsGuion.length == 2) {
                    int ini = Integer.parseInt(partsGuion[0]);
                    int fin = Integer.parseInt(partsGuion[1]);
                    if (fin < ini) {
                        fin = Integer.parseInt(partsGuion[0]);
                        ini = Integer.parseInt(partsGuion[1]);
                    }
                    if (fin > 65535) {
                        fin = 65535;
                    }
                    if (ini == 0) {
                        ini = 1;
                    }

                    for (int i = ini; i <= fin; i++) {

                        listaPuertos.add(i);
                    }

                } else {
                    return false;
                }

            } else {

                if (Integer.parseInt(portsComa) > 0 && Integer.parseInt(portsComa) <= 65535) {
                    listaPuertos.add(Integer.parseInt(portsComa));
                }
            }
        }

        hs.addAll(listaPuertos);
        listaPuertos.clear();
        listaPuertos.addAll(hs);

        LogUtils.LOGI("Total puertos: " + listaPuertos.size());

        if (listaPuertos.size() == 0) {
            return false;
        }
        return true;
    }

    //Carga el sniper
    private void loadSnniper() {

        List<String> categories = new ArrayList<String>();
        categories.add("100");
        categories.add("200");
        categories.add("300");
        categories.add("500");
        categories.add("1000");
        categories.add("2000");
        categories.add("0");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

    }


    //esconce el teclado al clicar en el view que le pasas
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);

    }

    public void setupUI(View view) {

        //Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {

            view.setOnTouchListener(new View.OnTouchListener() {

                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(getActivity());
                    return false;
                }

            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {

            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {

                View innerView = ((ViewGroup) view).getChildAt(i);

                setupUI(innerView);
            }
        }

    }

    /**********************/
    private class ObtenerIp extends AsyncTask<String, Void, String> {

        ProgressDialog dialog;
        Activity ac;

        public ObtenerIp(Activity ac) {
            this.ac = ac;
            dialog = new ProgressDialog(ac);
        }

        public ObtenerIp() {
        }


        protected void onPreExecute() {
            if (ac != null)
                this.dialog.setMessage(ac.getString(R.string.procesando));
            else
                cancel(true);
            this.dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {

            InetAddress address = null;
            String retorno = "";

            resultadoParseaPorts = parsea(params[0]);

            try {
                address = InetAddress.getByName(params[1]);
                retorno = address.getHostAddress().toString();
            } catch (UnknownHostException e) {
                retorno = "";
            }
            LogUtils.LOGI("La IP: " + retorno);
            return retorno;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    private void aviso(int mensaje) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(mensaje)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.create().show();


    }


    private void aviso2(String mensaje) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(mensaje)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.create().show();


    }
}