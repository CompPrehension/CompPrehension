package org.vstu.compprehension.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;

public class SessionHelper {
    public static HttpSession ensureNewSession(HttpServletRequest request) {
        var oldSession = request.getSession();
        if (oldSession.isNew()) {
            return oldSession;
        }

        // copy scopeTarget attributes from old session to new
        var oldAttributes = new HashMap<String, Object>();
        var attributeNames = request.getSession().getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            if (!attributeName.startsWith("scopedTarget."))
                continue;
            Object attributeValue = request.getSession().getAttribute(attributeName);
            oldAttributes.put(attributeName, attributeValue);
        }

        oldSession.invalidate();
        var newSession = request.getSession();
        for (var attr : oldAttributes.keySet()) {
            if (newSession.getAttribute(attr) == null)
                newSession.setAttribute(attr, oldAttributes.get(attr));
        }
        return newSession;
    }
}
