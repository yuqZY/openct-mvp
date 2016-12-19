package cc.metapro.openct.university.library;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Strings;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import cc.metapro.openct.data.BookInfo;
import cc.metapro.openct.data.BorrowInfo;
import cc.metapro.openct.libsearch.LibSearchPresenter;
import cc.metapro.openct.university.UniversityHelper;
import cc.metapro.openct.university.UniversityInfo;
import cc.metapro.openct.utils.Constants;
import cc.metapro.openct.utils.OkCurl;

/**
 * Created by jeffrey on 11/23/16.
 */

public abstract class AbstractLibrary {

    private final static Pattern loginSuccessPattern = Pattern.compile("当前借阅");

    private final static Pattern nextPagePattern = Pattern.compile("(下一页)");
    private static String queryTmp, nextPageURL;
    protected UniversityInfo.LibraryInfo mLibraryInfo;
    // Strings related to login
    protected String mLoginURL, mCaptchaURL, mUserCenterURL, mLoginReferer;

    // Strings related to search
    protected String mSearchRefer, mSearchURL;

    @Nullable
    protected String login(@NonNull Map<String, String> loginMap) throws IOException, LoginException {
        Map<String, String> res = UniversityHelper.
                formLoginPostContent(loginMap, mLoginURL);

        if (res == null) return null;

        String content = res.get(UniversityHelper.CONTENT);
        String action = res.get(UniversityHelper.ACTION);

        // post to login
        Map<String, String> headers = new HashMap<>(1);
        headers.put("Referer", action);
        String userCenter = OkCurl.curlSynPOST(action, headers, Constants.POST_CONTENT_TYPE_FORM_URLENCODED, content).body().string();

        if (loginSuccessPattern.matcher(userCenter).find()) {
            return userCenter;
        } else {

            throw new LoginException("login fail");
        }
    }

    public void getCODE(@NonNull String path) throws IOException {
        Map<String, String> headers = new HashMap<>(1);
        headers.put("Referer", mLoginReferer);
        OkCurl.curlSynGET(mCaptchaURL, headers, path);
    }

    @Nullable
    public List<BookInfo> search(@NonNull Map<String, String> searchMap) throws IOException {
        queryTmp = null;
        nextPageURL = null;
        searchMap.put(Constants.SEARCH_TYPE, typeTrans(searchMap.get(Constants.SEARCH_CONTENT)));
        Map<String, String> res = UniversityHelper.
                formSearchGetContent(searchMap, mSearchRefer);

        if (res == null) throw new IOException("network error");

        String content = res.get(UniversityHelper.CONTENT);
        String action = res.get(UniversityHelper.ACTION);

        queryTmp = action + "?" + content;
        Map<String, String> map = new HashMap<>(1);
        map.put("Referer", mSearchRefer);
        String resultPage = OkCurl.curlSynGET(queryTmp, map, null).body().string();

        findNextPageURL(resultPage);

        return Strings.isNullOrEmpty(resultPage) ? null : parseBook(resultPage);
    }

    @Nullable
    public List<BookInfo> getNextPage() throws IOException {
        Map<String, String> map = new HashMap<>(1);
        map.put("Referer", queryTmp);
        String reslutPage = OkCurl.curlSynGET(nextPageURL, map, null).body().string();

        findNextPageURL(reslutPage);
        return Strings.isNullOrEmpty(reslutPage) ? null : parseBook(reslutPage);
    }

    private void findNextPageURL(String resultPage) {
        Document result = Jsoup.parse(resultPage, mSearchURL);
        Elements links = result.select("a");
        for (Element a : links) {
            if (nextPagePattern.matcher(a.text()).find()) {
                nextPageURL = a.absUrl("href");
            }
        }
    }

    protected abstract List<BookInfo> parseBook(@NonNull String resultPage);

    @Nullable
    public abstract List<BorrowInfo> getBorrowInfo(@NonNull Map<String, String> loginMap) throws IOException, LoginException;

    @NonNull
    protected abstract String typeTrans(String cnType);

    public static class BorrowTableInfo {
        public int
                mBarcodeIndex,
                mTitleIndex,
                mBorrowDateIndex,
                mDueDateIndexIndex;
        public String mTableID;
    }

}