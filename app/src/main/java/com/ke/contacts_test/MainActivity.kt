package com.ke.contacts_test

import android.Manifest
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v7.app.AppCompatActivity
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.tbruyelle.rxpermissions2.RxPermissions
import android.content.ContentResolver
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initLogger()

        val permission = RxPermissions(this)
        permission.request(Manifest.permission.READ_CONTACTS)
                .subscribe({ value ->
                    if (value) {
                        Observable.just(1)
                                .observeOn(Schedulers.io())
                                .subscribe({
                                    loadContacts()
                                })
                    } else {
                        Logger.d("grant fail")
                    }
                }, { e ->
                    e.printStackTrace()
                })


    }


    private fun initLogger() {
        Logger.addLogAdapter(AndroidLogAdapter())
    }

    private fun loadContacts() {
        Logger.d("start load contacts")

        val contactsCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
        contactsCursor.moveToFirst()


        while (contactsCursor.moveToNext()) {
//            loggerCursor(contactsCursor)
            val contactId = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID))

            Logger.d("contact id $contactId")

//            logData(contentResolver, contactId)

            val displayName = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

            Logger.d("displayName = $displayName")

            val phonePairs: MutableList<Pair<String, String>> = mutableListOf()
            val phonesCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.DATA3), ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null)
            phonesCursor.moveToFirst()
            while (phonesCursor.moveToNext()) {
                //读取所有的电话信息
                val phoneNumber = phonesCursor.getString(0)

                val phoneType = getTypeName(phonesCursor, 1, 2)

                phonePairs.add(Pair(phoneType, phoneNumber))

            }
            phonesCursor.close()

            loggerSection(phonePairs, "phone")


            val emailCursor = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.DATA3), ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + contactId, null, null)
            val emailPairs: MutableList<Pair<String, String>> = mutableListOf()
            emailCursor.moveToFirst()

            while (emailCursor.moveToNext()) {

//                loggerCursor(emailCursor)
                val emailType = getTypeName(emailCursor, 1, 2)
                val emailAddress = emailCursor.getString(0)

                emailPairs.add(Pair(emailType, emailAddress))
            }

            emailCursor.close()

            loggerSection(emailPairs, "email")


            val addressCursor = contentResolver.query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.StructuredPostal.DATA, ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.DATA3), ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = " + contactId, null, null)
            addressCursor.moveToFirst()
            val addressPairs: MutableList<Pair<String, String>> = mutableListOf()


            while (addressCursor.moveToNext()) {
                val address = addressCursor.getString(0)

                val addressType = getTypeName(addressCursor, 1, 2)

                addressPairs.add(Pair(addressType, address))

            }

            addressCursor.close()

            loggerSection(addressPairs, "address")

            //备注
            val note = getContactsMimeData(contactId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)

            Logger.d("note = $note")

            //网站
            val websites = getContactsMimeDatas(contactId, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)

            Logger.d("websites = $websites")

            //公司和职位
            val companyTitle = getContactsCompanyAndTitle(contactId)

            Logger.d("company = ${companyTitle.first} , title = ${companyTitle.second}")

            //sip
            val sip = getContactsMimeData(contactId, ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE)

            Logger.d("sip address = $sip")

            //通讯软件
            val ims = getContactsIMS(contactId)

            loggerSection(ims, "IM")


//            Logger.d("displayName = $displayName , contactId = $contactId , phones = " + getSections(phonePairs, "phone") + ",emails = " + getSections(emailPairs, "email") + "," + getSections(addressPairs, "address"))


        }

        contactsCursor.close()
    }


    /**
     * 返回类型名称
     */
    private fun getTypeName(cursor: Cursor, typeIndex: Int, customerTypeIndex: Int): String {
        val type = cursor.getString(typeIndex)
        return if (type == "0") {
            cursor.getString(customerTypeIndex)
        } else {
            type
        }

    }


    private fun loggerCursor(cursor: Cursor) {
        Logger.d("==================")
        for (columnName in cursor.columnNames) {
            val columnValue = cursor.getString(cursor.getColumnIndex(columnName))

            Logger.d("key = $columnName , value = $columnValue")
        }
    }

    private fun getSections(list: List<Pair<String, String>>, title: String): CharSequence {
        val message = StringBuilder()

        for (pair in list) {
            message.append("$title Type=${pair.first},$title value=${pair.second} .")
        }

        return message
    }


    private fun getContactsIMS(id: String): List<Pair<String, String>> {

        val projection = arrayOf(ContactsContract.CommonDataKinds.Im.DATA, ContactsContract.CommonDataKinds.Im.TYPE, ContactsContract.CommonDataKinds.Im.DATA3)

        val section = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"
        val sectionArgs = arrayOf(id,ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
        val cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, section, sectionArgs, null)

        val pairs = mutableListOf<Pair<String, String>>()

        cursor.moveToFirst()

        while (cursor.moveToNext()) {
            pairs.add(Pair(getTypeName(cursor, 1, 2), cursor.getString(0)))
        }

        cursor.close()

        return pairs
    }


    private fun loggerSection(list: List<Pair<String, String>>, title: String) {
        val value = getSections(list, title)

        Logger.d("$title = {$value}")
    }

//    private fun getContactsWebsites(id: String):List<String>{
//        val cursor = contentResolver.query(ContactsContract.CommonDataKinds.Website.)
//    }

    /**
     * 获取公司名称和职位
     */
    private fun getContactsCompanyAndTitle(id: String): Pair<String, String> {
        val orgWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"
        val orgWhereParams = arrayOf(id, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
        val cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Organization.DATA, ContactsContract.CommonDataKinds.Organization.TITLE), orgWhere, orgWhereParams, null)

        val pair: Pair<String, String> = if (cursor.moveToFirst()) {
            Pair(cursor.getString(0), cursor.getString(1))
        } else {
            Pair("", "")
        }

        cursor.close()

        return pair
    }


    private fun getContactsMimeDatas(id: String, mimeType: String): List<String> {
        val cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, arrayOf(ContactsContract.Data.DATA1), ContactsContract.Data.CONTACT_ID + "=?" + " AND "
                + ContactsContract.Data.MIMETYPE + "='" + mimeType + "'", arrayOf(id), null)

        val dataList = mutableListOf<String>()

        cursor.moveToFirst()

        while (cursor.moveToNext()) {
            dataList.add(cursor.getString(0))
        }


        cursor.close()

        return dataList
    }

    //联系人的某个字段
    private fun getContactsMimeData(id: String, mimeType: String): String {
        val cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, arrayOf(ContactsContract.Data.DATA1), ContactsContract.Data.CONTACT_ID + "=?" + " AND "
                + ContactsContract.Data.MIMETYPE + "='" + mimeType + "'", arrayOf(id), null)


        val result = if (cursor.moveToFirst()) {
            cursor.getString(0)
        } else {
            "null"
        }

        cursor.close()

        return result

    }

    private fun logData(contentResolver: ContentResolver, contactId: String) {
        val dataCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null,
                ContactsContract.Data.CONTACT_ID + "=?",
                arrayOf(contactId), null)
        if (dataCursor != null) {
            if (dataCursor.count > 0) {
                Logger.d("----------------------start--------------------")
                Logger.d("数量:" + dataCursor.count + " 列数:" + dataCursor.columnCount)
                if (dataCursor.moveToFirst()) {
                    do {
                        for (i in 0 until dataCursor.columnCount) {
                            val columnName = dataCursor.getColumnName(i)
                            val columnIndex = dataCursor.getColumnIndex(columnName)
                            val type = dataCursor.getType(columnIndex)
                            var data = ""
                            var ty = ""
                            if (type == Cursor.FIELD_TYPE_NULL) {
                                ty = "NULL"
                                data = "空值"
                            } else if (type == Cursor.FIELD_TYPE_BLOB) {
                                ty = "BLOB"
                                data = dataCursor.getBlob(columnIndex).toString()
                            } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                                ty = "FLOAT"
                                data = dataCursor.getFloat(columnIndex).toString()
                            } else if (type == Cursor.FIELD_TYPE_INTEGER) {
                                ty = "INTEGER"
                                data = dataCursor.getInt(columnIndex).toString()
                            } else if (type == Cursor.FIELD_TYPE_STRING) {
                                ty = "STRING"
                                data = dataCursor.getString(columnIndex)
                            }

                            Logger.d("第" + i + "列->名称:" + columnName + " 索引:" + columnIndex + " 类型:" + ty + " 值:" + data)
                        }
                    } while (dataCursor.moveToNext())
                }
                Logger.d("------------------------end---------------------")
            }
            dataCursor.close()
        }
    }


}
