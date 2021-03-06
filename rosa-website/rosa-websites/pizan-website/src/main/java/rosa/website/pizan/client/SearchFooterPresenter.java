package rosa.website.pizan.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.AdvancedSearchPlace;
import rosa.website.core.client.view.SearchFooterView;

public class SearchFooterPresenter implements SearchFooterView.Presenter, IsWidget {
    private static final Labels labels = Labels.INSTANCE;

    private final SearchFooterView view;

    public SearchFooterPresenter(final ClientFactory clientFactory) {
        this.view = clientFactory.searchFooterView();

        view.setSearchButtonText(labels.search());
        view.addAdvancedSearchLink(labels.advancedSearch(), "search;");
        view.addSearchClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String searchToken = view.getSearchToken();

                if (searchToken != null && !searchToken.trim().isEmpty()) {
                    clientFactory.placeController().goTo(new AdvancedSearchPlace(searchToken));
                }
            }
        });

        view.addSearchKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getUnicodeCharCode() != KeyCodes.KEY_ENTER) {
                    return;
                }

                String searchToken = view.getSearchToken();
                if (searchToken != null && !searchToken.trim().isEmpty()) {
                    clientFactory.placeController().goTo(new AdvancedSearchPlace(searchToken));
                }
            }
        });
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }
}
