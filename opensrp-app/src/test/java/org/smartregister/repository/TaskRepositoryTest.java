package org.smartregister.repository;

import android.content.ContentValues;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.sqlcipher.MatrixCursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SQLiteStatement;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.smartregister.BaseUnitTest;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.domain.TaskUpdate;
import org.smartregister.domain.db.Client;
import org.smartregister.util.DateTimeTypeConverter;
import org.smartregister.view.activity.DrishtiApplication;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.smartregister.domain.Task.TaskStatus.ARCHIVED;
import static org.smartregister.domain.Task.TaskStatus.CANCELLED;
import static org.smartregister.domain.Task.TaskStatus.READY;
import static org.smartregister.repository.TaskRepository.TASK_TABLE;

/**
 * Created by samuelgithengi on 11/26/18.
 */

public class TaskRepositoryTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private TaskRepository taskRepository;

    @Mock
    private Repository repository;
    @Mock
    private TaskNotesRepository taskNotesRepository;

    @Mock
    private SQLiteDatabase sqLiteDatabase;

    @Captor
    private ArgumentCaptor<ContentValues> contentValuesArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Captor
    private ArgumentCaptor<String[]> argsCaptor;

    @Mock
    private SQLiteStatement sqLiteStatement;

    @Captor
    private ArgumentCaptor<String[]> stringArrayArgumentCaptor;

    @Captor
    private ArgumentCaptor<Task> taskArgumentCaptor;

    private String taskJson = "{\"identifier\":\"tsk11231jh22\",\"planIdentifier\":\"IRS_2018_S1\",\"groupIdentifier\":\"2018_IRS-3734\",\"status\":\"Ready\",\"businessStatus\":\"Not Visited\",\"priority\":3,\"code\":\"IRS\",\"description\":\"Spray House\",\"focus\":\"IRS Visit\",\"for\":\"location.properties.uid:41587456-b7c8-4c4e-b433-23a786f742fc\",\"executionStartDate\":\"2018-11-10T2200\",\"executionEndDate\":null,\"authoredOn\":\"2018-10-31T0700\",\"lastModified\":\"2018-10-31T0700\",\"owner\":\"demouser\",\"note\":[{\"authorString\":\"demouser\",\"time\":\"2018-01-01T0800\",\"text\":\"This should be assigned to patrick.\"}],\"serverVersion\":0,\"structureId\":\"structure._id.33efadf1-feda-4861-a979-ff4f7cec9ea7\",\"reasonReference\":\"fad051d9-0ff6-424a-8a44-4b90883e2841\"}";
    private String structureJson = "{\"id\": \"170230\", \"type\": \"Feature\", \"geometry\": {\"type\": \"Point\", \"coordinates\": [32.59610261651737, -14.171511296715634]}, \"properties\": {\"status\": \"Active\", \"version\": 0, \"parentId\": \"3429\", \"geographicLevel\": 4}, \"serverVersion\": 1542970626353}";
    private String clientJson = "{\"firstName\":\"Khumpai\",\"lastName\":\"Family\",\"birthdate\":\"1970-01-01T05:00:00.000+03:00\",\"birthdateApprox\":false,\"deathdateApprox\":false,\"gender\":\"Male\",\"relationships\":{\"family_head\":[\"7d97182f-d623-4553-8651-5a29d2fe3f0b\"],\"primary_caregiver\":[\"7d97182f-d623-4553-8651-5a29d2fe3f0b\"]},\"baseEntityId\":\"71ad460c-bf76-414e-9be1-0d1b2cb1bce8\",\"identifiers\":{\"opensrp_id\":\"11096120_family\"},\"addresses\":[{\"addressType\":\"\",\"cityVillage\":\"Tha Luang\"}],\"attributes\":{\"residence\":\"da765947-5e4d-49f7-9eb8-2d2d00681f65\"},\"dateCreated\":\"2019-05-12T17:22:31.023+03:00\",\"serverVersion\":1557670950986,\"clientApplicationVersion\":2,\"clientDatabaseVersion\":2,\"type\":\"Client\",\"id\":\"9b67a82d-dac7-40c0-85aa-e5976339a6b6\",\"revision\":\"v1\"}";

    private static Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new DateTimeTypeConverter("yyyy-MM-dd'T'HHmm"))
            .serializeNulls().create();

    private static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HHmm");

    @Before
    public void setUp() {

        taskRepository = new TaskRepository(taskNotesRepository);
        when(repository.getReadableDatabase()).thenReturn(sqLiteDatabase);
        when(repository.getWritableDatabase()).thenReturn(sqLiteDatabase);
        when(repository.getWritableDatabase().compileStatement(anyString())).thenReturn(sqLiteStatement);
        Whitebox.setInternalState(DrishtiApplication.getInstance(), "repository", repository);
    }

    @Test
    public void testAddOrUpdateShouldAdd() {

        Task task = gson.fromJson(taskJson, Task.class);
        taskRepository.addOrUpdate(task);

        verify(sqLiteDatabase).replace(stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), contentValuesArgumentCaptor.capture());

        Iterator<String> iterator = stringArgumentCaptor.getAllValues().iterator();
        assertEquals(TASK_TABLE, iterator.next());
        assertNull(iterator.next());

        ContentValues contentValues = contentValuesArgumentCaptor.getValue();
        assertEquals(21, contentValues.size());

        assertEquals("tsk11231jh22", contentValues.getAsString("_id"));
        assertEquals("IRS_2018_S1", contentValues.getAsString("plan_id"));
        assertEquals("2018_IRS-3734", contentValues.getAsString("group_id"));

        verify(taskNotesRepository).addOrUpdate(task.getNotes().get(0), task.getIdentifier());


    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddOrUpdateShouldThrowException() {

        Task task = new Task();
        taskRepository.addOrUpdate(task);

    }

    @Test
    public void testGetTasksByPlanAndGroup() {
        when(sqLiteDatabase.rawQuery("SELECT * FROM task WHERE plan_id=? AND group_id =? AND status NOT IN (?,?)",
                new String[]{"IRS_2018_S1", "2018_IRS-3734", CANCELLED.name(), ARCHIVED.name()})).thenReturn(getCursor());
        Map<String, Set<Task>> allTasks = taskRepository.getTasksByPlanAndGroup("IRS_2018_S1", "2018_IRS-3734");
        verify(sqLiteDatabase).rawQuery(stringArgumentCaptor.capture(), argsCaptor.capture());

        assertEquals("SELECT * FROM task WHERE plan_id=? AND group_id =? AND status NOT IN (?,?)", stringArgumentCaptor.getValue());

        assertEquals("IRS_2018_S1", argsCaptor.getValue()[0]);
        assertEquals("2018_IRS-3734", argsCaptor.getValue()[1]);
        assertEquals(CANCELLED.name(), argsCaptor.getValue()[2]);
        assertEquals(ARCHIVED.name(), argsCaptor.getValue()[3]);

        assertEquals(1, allTasks.size());
        assertEquals(1, allTasks.get("structure._id.33efadf1-feda-4861-a979-ff4f7cec9ea7").size());
        Task task = allTasks.get("structure._id.33efadf1-feda-4861-a979-ff4f7cec9ea7").iterator().next();

        assertEquals("tsk11231jh22", task.getIdentifier());
        assertEquals("2018_IRS-3734", task.getGroupIdentifier());
        assertEquals(READY, task.getStatus());
        assertEquals("Not Visited", task.getBusinessStatus());
        assertEquals(3, task.getPriority());
        assertEquals("IRS", task.getCode());
        assertEquals("Spray House", task.getDescription());
        assertEquals("IRS Visit", task.getFocus());
        assertEquals("location.properties.uid:41587456-b7c8-4c4e-b433-23a786f742fc", task.getForEntity());
        assertEquals("2018-11-10T2200", task.getExecutionStartDate().toString(formatter));
        assertNull(task.getExecutionEndDate());
        assertEquals("2018-10-31T0700", task.getAuthoredOn().toString(formatter));
        assertEquals("2018-10-31T0700", task.getLastModified().toString(formatter));
        assertEquals("demouser", task.getOwner());

    }

    @Test
    public void testGetTasksByEntityAndCode() {
        when(sqLiteDatabase.rawQuery("SELECT * FROM task WHERE plan_id=? AND group_id =? AND for =?  AND code =? AND status  NOT IN (?,?)",
                new String[]{"IRS_2018_S1", "2018_IRS-3734", "location.properties.uid:41587456-b7c8-4c4e-b433-23a786f742fc", "IRS", CANCELLED.name(), ARCHIVED.name()})).thenReturn(getCursor());
        Set<Task> allTasks = taskRepository.getTasksByEntityAndCode("IRS_2018_S1", "2018_IRS-3734", "location.properties.uid:41587456-b7c8-4c4e-b433-23a786f742fc", "IRS");
        verify(sqLiteDatabase).rawQuery(stringArgumentCaptor.capture(), argsCaptor.capture());

        assertEquals("SELECT * FROM task WHERE plan_id=? AND group_id =? AND for =?  AND code =? AND status  NOT IN (?,?)", stringArgumentCaptor.getValue());

        assertEquals("IRS_2018_S1", argsCaptor.getValue()[0]);
        assertEquals("2018_IRS-3734", argsCaptor.getValue()[1]);
        assertEquals("location.properties.uid:41587456-b7c8-4c4e-b433-23a786f742fc", argsCaptor.getValue()[2]);
        assertEquals("IRS", argsCaptor.getValue()[3]);
        assertEquals(CANCELLED.name(), argsCaptor.getValue()[4]);
        assertEquals(ARCHIVED.name(), argsCaptor.getValue()[5]);

        assertEquals(1, allTasks.size());
        Task task = allTasks.iterator().next();

        assertEquals("tsk11231jh22", task.getIdentifier());
        assertEquals("2018_IRS-3734", task.getGroupIdentifier());
        assertEquals(READY, task.getStatus());
        assertEquals("Not Visited", task.getBusinessStatus());
        assertEquals(3, task.getPriority());
        assertEquals("IRS", task.getCode());
        assertEquals("Spray House", task.getDescription());
        assertEquals("IRS Visit", task.getFocus());
        assertEquals("location.properties.uid:41587456-b7c8-4c4e-b433-23a786f742fc", task.getForEntity());
        assertEquals("2018-11-10T2200", task.getExecutionStartDate().toString(formatter));
        assertNull(task.getExecutionEndDate());
        assertEquals("2018-10-31T0700", task.getAuthoredOn().toString(formatter));
        assertEquals("2018-10-31T0700", task.getLastModified().toString(formatter));
        assertEquals("demouser", task.getOwner());

    }

    @Test
    public void testGetTaskByIdentifier() {

        when(sqLiteDatabase.rawQuery("SELECT * FROM task WHERE _id =?", new String[]{"tsk11231jh22"})).thenReturn(getCursor());
        Task task = taskRepository.getTaskByIdentifier("tsk11231jh22");
        verify(sqLiteDatabase).rawQuery(stringArgumentCaptor.capture(), argsCaptor.capture());
        assertEquals("SELECT * FROM task WHERE _id =?", stringArgumentCaptor.getValue());
        assertEquals(1, argsCaptor.getValue().length);
        assertEquals("tsk11231jh22", argsCaptor.getValue()[0]);


        assertEquals("tsk11231jh22", task.getIdentifier());
        assertEquals("2018_IRS-3734", task.getGroupIdentifier());
        assertEquals(READY, task.getStatus());
        assertEquals("Not Visited", task.getBusinessStatus());
        assertEquals(3, task.getPriority());
        assertEquals("IRS", task.getCode());
        assertEquals("Spray House", task.getDescription());
        assertEquals("IRS Visit", task.getFocus());
        assertEquals("location.properties.uid:41587456-b7c8-4c4e-b433-23a786f742fc", task.getForEntity());
        assertEquals("2018-11-10T2200", task.getExecutionStartDate().toString(formatter));
        assertNull(task.getExecutionEndDate());
        assertEquals("2018-10-31T0700", task.getAuthoredOn().toString(formatter));
        assertEquals("2018-10-31T0700", task.getLastModified().toString(formatter));
        assertEquals("demouser", task.getOwner());


    }


    public MatrixCursor getCursor() {
        MatrixCursor cursor = new MatrixCursor(TaskRepository.COLUMNS);
        Task task = gson.fromJson(taskJson, Task.class);

        cursor.addRow(new Object[]{task.getIdentifier(), task.getPlanIdentifier(), task.getGroupIdentifier(),
                task.getStatus().name(), task.getBusinessStatus(), task.getPriority(), task.getCode(),
                task.getDescription(), task.getFocus(), task.getForEntity(),
                task.getExecutionStartDate().getMillis(),
                null,
                task.getAuthoredOn().getMillis(), task.getLastModified().getMillis(),
                task.getOwner(), task.getSyncStatus(), task.getServerVersion(), task.getStructureId(), task.getReasonReference(), null, null});
        return cursor;
    }

    @Test
    public void testGetUnSyncedTaskStatus() {
        taskRepository.getUnSyncedTaskStatus();
        verify(sqLiteDatabase).rawQuery(stringArgumentCaptor.capture(), argsCaptor.capture());
        assertNotNull(taskRepository.getUnSyncedTaskStatus());
    }

    @Test
    public void testUpdateTaskStructureIdFromClient() throws Exception {
        List<Client> clients = new ArrayList<>();
        Client client = gson.fromJson(clientJson, Client.class);
        clients.add(client);
        taskRepository.updateTaskStructureIdFromClient(clients, "");
        assertNotNull(taskRepository.getUnSyncedTaskStatus());
    }

    @Test
    public void updateTaskStructureIdFromStructure() throws Exception {
        List<Location> locations = new ArrayList<>();
        Location location = gson.fromJson(structureJson, Location.class);
        locations.add(location);

        taskRepository.updateTaskStructureIdFromStructure(locations);
        assertTrue(taskRepository.updateTaskStructureIdFromStructure(locations));
    }


    @Test
    public void testCancelTasksForEntity() {
        taskRepository.cancelTasksForEntity("id1");
        verify(sqLiteDatabase).update(eq(TASK_TABLE), contentValuesArgumentCaptor.capture(), eq("for = ? AND status =?"), eq(new String[]{"id1", READY.name()}));
        assertEquals(BaseRepository.TYPE_Unsynced, contentValuesArgumentCaptor.getValue().getAsString("sync_status"));
        assertEquals(CANCELLED.name(), contentValuesArgumentCaptor.getValue().getAsString("status"));
        assertEquals(2, contentValuesArgumentCaptor.getValue().size());
    }

    @Test
    public void testCancelTasksForEntityWithNullParams() {
        taskRepository.cancelTasksForEntity(null);
        verify(sqLiteDatabase, never()).update(any(), any(), any(), any());
        verifyZeroInteractions(sqLiteDatabase);
    }


    @Test
    public void testArchiveTasksForEntity() {
        taskRepository.archiveTasksForEntity("id1");
        verify(sqLiteDatabase).update(eq(TASK_TABLE), contentValuesArgumentCaptor.capture(), eq("for = ? AND status NOT IN (?,?)"), eq(new String[]{"id1", READY.name(), CANCELLED.name()}));
        assertEquals(BaseRepository.TYPE_Unsynced, contentValuesArgumentCaptor.getValue().getAsString("sync_status"));
        assertEquals(ARCHIVED.name(), contentValuesArgumentCaptor.getValue().getAsString("status"));
        assertEquals(2, contentValuesArgumentCaptor.getValue().size());
    }

    @Test
    public void testArchiveTasksForEntityWithNullParams() {
        taskRepository.archiveTasksForEntity(null);
        verifyZeroInteractions(sqLiteDatabase);
    }

    @Test
    public void testReadUpdateCursor() {
        MatrixCursor cursor = getCursor();
        cursor.moveToNext();
        String expectedIdentifier = cursor.getString(cursor.getColumnIndex("_id"));
        String expectedStatus = cursor.getString(cursor.getColumnIndex("status"));
        String expectedBusinessStatus = cursor.getString(cursor.getColumnIndex("business_status"));
        String expectedServerVersion = cursor.getString(cursor.getColumnIndex("server_version"));

        TaskUpdate returnedTaskUpdate = taskRepository.readUpdateCursor(cursor);

        assertNotNull(returnedTaskUpdate);
        assertEquals(expectedIdentifier, returnedTaskUpdate.getIdentifier());
        assertEquals(expectedStatus, returnedTaskUpdate.getStatus());
        assertEquals(expectedBusinessStatus, returnedTaskUpdate.getBusinessStatus());
        assertEquals(expectedServerVersion, returnedTaskUpdate.getServerVersion());

    }

    @Test
    public void testMarkTaskAsSynced() {

        String expectedTaskIdentifier = "id1";
        taskRepository.markTaskAsSynced(expectedTaskIdentifier);

        verify(sqLiteDatabase).update(stringArgumentCaptor.capture(), contentValuesArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArrayArgumentCaptor.capture());

        Iterator<String> iterator = stringArgumentCaptor.getAllValues().iterator();
        assertEquals(TaskRepository.TASK_TABLE, iterator.next());
        assertEquals("_id = ?", iterator.next());

        ContentValues contentValues = contentValuesArgumentCaptor.getValue();
        assertEquals(3, contentValues.size());
        assertEquals(expectedTaskIdentifier, contentValues.getAsString("_id"));
        assertEquals(BaseRepository.TYPE_Synced, contentValues.getAsString("sync_status"));
        assertEquals(0, contentValues.getAsInteger("server_version").intValue());

        String actualTaskIdentifier = stringArrayArgumentCaptor.getAllValues().get(0)[0];
        assertEquals(expectedTaskIdentifier, actualTaskIdentifier);

    }

    @Test
    public void testGetAllUnSyncedCreatedTasks() {

        when(sqLiteDatabase.rawQuery("SELECT *  FROM task WHERE sync_status =? OR server_version IS NULL", new String[]{BaseRepository.TYPE_Created})).thenReturn(getCursor());

        List<Task> unsyncedCreatedTasks = taskRepository.getAllUnsynchedCreatedTasks();
        assertEquals(1, unsyncedCreatedTasks.size());

        Task actualTask = unsyncedCreatedTasks.get(0);

        assertEquals("tsk11231jh22", actualTask.getIdentifier());
        assertEquals("2018_IRS-3734", actualTask.getGroupIdentifier());
        assertEquals(READY, actualTask.getStatus());
        assertEquals("Not Visited", actualTask.getBusinessStatus());
        assertEquals(3, actualTask.getPriority());
        assertEquals("IRS", actualTask.getCode());
        assertEquals("Spray House", actualTask.getDescription());
        assertEquals("IRS Visit", actualTask.getFocus());
        assertEquals("location.properties.uid:41587456-b7c8-4c4e-b433-23a786f742fc", actualTask.getForEntity());
        assertEquals("2018-11-10T2200", actualTask.getExecutionStartDate().toString(formatter));
        assertNull(actualTask.getExecutionEndDate());
        assertEquals("2018-10-31T0700", actualTask.getAuthoredOn().toString(formatter));
        assertEquals("2018-10-31T0700", actualTask.getLastModified().toString(formatter));
        assertEquals("demouser", actualTask.getOwner());

    }

    @Test
    public void testUpdateTaskStructureIdsFromExistingStructures() {

        String expectedSql = "UPDATE task SET structure_id =(SELECT _id FROM structure WHERE _id = for) WHERE structure_id IS NULL";
        boolean updated = taskRepository.updateTaskStructureIdsFromExistingStructures();

        assertTrue(updated);
        verify(sqLiteDatabase).execSQL(stringArgumentCaptor.capture());
        assertEquals(expectedSql, stringArgumentCaptor.getValue());

    }

    @Test
    public void testUpdateTaskStructureIdsFromExistingStructuresFailure() {

        String expectedSql = "UPDATE task SET structure_id =(SELECT _id FROM structure WHERE _id = for) WHERE structure_id IS NULL";

        doThrow(new SQLiteException()).when(sqLiteDatabase).execSQL(anyString());

        boolean updated = taskRepository.updateTaskStructureIdsFromExistingStructures();

        assertFalse(updated);
        verify(sqLiteDatabase).execSQL(stringArgumentCaptor.capture());
        assertEquals(expectedSql, stringArgumentCaptor.getValue());
    }

    @Test
    public void testUpdateTaskStructureIdsfromExistingClients() {

        String expectedSql = "UPDATE task SET structure_id =(SELECT structure_id FROM ec_family_member WHERE base_entity_id = for) WHERE structure_id IS NULL";
        String clientTable = "ec_family_member";
        boolean updated = taskRepository.updateTaskStructureIdsFromExistingClients(clientTable);

        assertTrue(updated);
        verify(sqLiteDatabase).execSQL(stringArgumentCaptor.capture());
        assertEquals(expectedSql, stringArgumentCaptor.getValue());

    }

    @Test
    public void testUpdateTaskStructureIdsfromExistingClientsFailure() {

        String expectedSql = "UPDATE task SET structure_id =(SELECT structure_id FROM ec_family_member WHERE base_entity_id = for) WHERE structure_id IS NULL";
        String clientTable = "ec_family_member";

        doThrow(new SQLiteException()).when(sqLiteDatabase).execSQL(anyString());

        boolean updated = taskRepository.updateTaskStructureIdsFromExistingClients(clientTable);

        assertFalse(updated);
        verify(sqLiteDatabase).execSQL(stringArgumentCaptor.capture());
        assertEquals(expectedSql, stringArgumentCaptor.getValue());

    }

    @Test
    public void testBatchInsertTasks() throws Exception {

        Task expectedTask = gson.fromJson(taskJson, Task.class);
        JSONArray taskArray = new JSONArray().put(new JSONObject(taskJson));

        taskRepository = spy(taskRepository);
        boolean inserted = taskRepository.batchInsertTasks(taskArray);

        verify(sqLiteDatabase).beginTransaction();
        verify(sqLiteDatabase).setTransactionSuccessful();
        verify(sqLiteDatabase).endTransaction();
        assertTrue(inserted);

        verify(taskRepository).addOrUpdate(taskArgumentCaptor.capture());
        assertEquals(expectedTask.getIdentifier(), taskArgumentCaptor.getValue().getIdentifier());
        assertEquals(expectedTask.getStatus(), taskArgumentCaptor.getValue().getStatus());
        assertEquals(expectedTask.getBusinessStatus(), taskArgumentCaptor.getValue().getBusinessStatus());
        assertEquals(expectedTask.getCode(), taskArgumentCaptor.getValue().getCode());
        assertEquals(expectedTask.getForEntity(), taskArgumentCaptor.getValue().getForEntity());

    }

    @Test
    public void testBatchInsertTasksWithNullParam() {

        taskRepository = spy(taskRepository);
        boolean inserted = taskRepository.batchInsertTasks(null);

        assertFalse(inserted);
        verify(sqLiteDatabase, never()).beginTransaction();
        verify(sqLiteDatabase, never()).setTransactionSuccessful();
        verify(sqLiteDatabase, never()).endTransaction();
        verify(taskRepository, never()).addOrUpdate(taskArgumentCaptor.capture());

    }

    @Test
    public void testBatchInsertTasksWithExceptionThrown() throws Exception {

        taskRepository = spy(taskRepository);
        JSONArray taskArray = new JSONArray().put(new JSONObject(taskJson));
        doThrow(new SQLiteException()).when(taskRepository).addOrUpdate(any());

        boolean inserted = taskRepository.batchInsertTasks(taskArray);

        assertFalse(inserted);
        verify(sqLiteDatabase).beginTransaction();
        verify(taskRepository).addOrUpdate(taskArgumentCaptor.capture());
        verify(sqLiteDatabase, never()).setTransactionSuccessful();
        verify(sqLiteDatabase).endTransaction();

    }

}
