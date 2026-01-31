import React from "react";
import { render, screen, waitFor } from "./test-utils";
import userEvent from "@testing-library/user-event";
import { ActivitySection } from "@/components/ActivitySection";
import * as activityApi from "@/lib/activityApi";

jest.mock("@/lib/activityApi");

const mockedActivityApi = activityApi as jest.Mocked<typeof activityApi>;

const mockEvents = {
  content: [
    {
      activityId: "evt-1",
      entityType: "LEAD" as const,
      entityId: "lead-1",
      eventType: "NOTE" as const,
      message: "Called customer",
      createdAt: "2024-01-15T14:00:00Z",
      createdByUserId: "user-1",
      createdByName: "Jane Doe",
      metadata: null,
    },
    {
      activityId: "evt-2",
      entityType: "LEAD" as const,
      entityId: "lead-1",
      eventType: "LEAD_STATUS_CHANGED" as const,
      message: "Status: NEW → CONTACTED",
      createdAt: "2024-01-14T10:00:00Z",
      createdByUserId: "user-1",
      createdByName: "Jane Doe",
      metadata: { fromStatus: "NEW", toStatus: "CONTACTED" },
    },
    {
      activityId: "evt-system",
      entityType: "LEAD" as const,
      entityId: "lead-1",
      eventType: "TASK_CREATED" as const,
      message: "Task created by system",
      createdAt: "2024-01-13T09:00:00Z",
      createdByUserId: null,
      createdByName: null,
      metadata: null,
    },
    {
      activityId: "evt-you",
      entityType: "LEAD" as const,
      entityId: "lead-1",
      eventType: "NOTE" as const,
      message: "Note by current user",
      createdAt: "2024-01-12T08:00:00Z",
      createdByUserId: "user-123",
      createdByName: null,
      metadata: null,
    },
    {
      activityId: "evt-attach",
      entityType: "LEAD" as const,
      entityId: "lead-1",
      eventType: "ATTACHMENT_ADDED" as const,
      message: "Added photo/doc (DAMAGE): damage.jpg",
      createdAt: "2024-01-11T07:00:00Z",
      createdByUserId: "user-1",
      createdByName: "Jane Doe",
      metadata: { attachmentId: "att-1", fileName: "damage.jpg", tag: "DAMAGE" },
    },
  ],
  totalElements: 5,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

describe("ActivitySection", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedActivityApi.listActivity.mockResolvedValue(mockEvents);
  });

  it("renders events returned by listActivity", async () => {
    render(<ActivitySection entityType="LEAD" entityId="lead-1" />);

    await waitFor(() => {
      expect(screen.getByText("Called customer")).toBeInTheDocument();
    });

    expect(screen.getAllByText("Note").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Status changed").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Status: NEW → CONTACTED")).toBeInTheDocument();
    expect(screen.getAllByText(/Jane Doe/).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/Task created by system/)).toBeInTheDocument();
    expect(screen.getByText(/Note by current user/)).toBeInTheDocument();
    expect(screen.getByText(/System/)).toBeInTheDocument();
    expect(screen.getByText(/You/)).toBeInTheDocument();
    expect(screen.getByText("Attachment added")).toBeInTheDocument();
    expect(screen.getByText(/Added photo\/doc \(DAMAGE\): damage\.jpg/)).toBeInTheDocument();
    expect(mockedActivityApi.listActivity).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        entityType: "LEAD",
        entityId: "lead-1",
        page: 0,
        size: 20,
      })
    );
  });

  it("creating a note calls createNote with correct payload and optimistic UI shows note", async () => {
    const newNote = {
      activityId: "evt-new",
      entityType: "LEAD" as const,
      entityId: "lead-1",
      eventType: "NOTE" as const,
      message: "New note text",
      createdAt: "2024-01-16T12:00:00Z",
      createdByUserId: "user-1",
      createdByName: "Jane Doe",
      metadata: null,
    };

    mockedActivityApi.createNote.mockResolvedValue(newNote);
    mockedActivityApi.listActivity
      .mockResolvedValueOnce(mockEvents)
      .mockResolvedValueOnce({
        ...mockEvents,
        content: [newNote, ...mockEvents.content],
        totalElements: 5,
      });

    render(<ActivitySection entityType="LEAD" entityId="lead-1" />);

    await waitFor(() => {
      expect(screen.getByText("Called customer")).toBeInTheDocument();
    });

    const textarea = screen.getByPlaceholderText("Add a note…");
    await userEvent.type(textarea, "New note text");

    const addButton = screen.getByRole("button", { name: /Add note/i });
    await userEvent.click(addButton);

    await waitFor(() => {
      expect(mockedActivityApi.createNote).toHaveBeenCalledWith(
        expect.anything(),
        { entityType: "LEAD", entityId: "lead-1", body: "New note text" }
      );
    });

    await waitFor(() => {
      expect(screen.getByText("New note text")).toBeInTheDocument();
    });
  });
});
